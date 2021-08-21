package jack.android.gradle.embedfunction.transform

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import jack.android.gradle.embedfunction.model.DecorateClassModel
import jack.android.gradle.embedfunction.visitor.DecoratedClassVisitor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.lang.NullPointerException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import kotlin.collections.HashSet

open class EmbedFunctionTransform(private val project: Project) : Transform() {

    override fun getName(): String {
        return "EmbedFunction"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return ImmutableSet.of(DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        println("Start transform the class files.")
        if (transformInvocation.isIncremental) {
            throw UnsupportedOperationException("Unsupported incremental build!")
        }
        transformInvocation.outputProvider.deleteAll()
        val decorateClassList = collectTheDecorateClasses(transformInvocation)
        decorateClassList.forEach { decorateClassModel ->
            println("\t"+decorateClassModel.targetClassName+"->"+decorateClassModel.decorateClassName)
        }
        transformJarFiles(transformInvocation, decorateClassList)
        transformClassFiles(transformInvocation, decorateClassList)
    }

    private fun collectTheDecorateClasses(transformInvocation: TransformInvocation): List<DecorateClassModel> {
        val decorateClassList = mutableListOf<DecorateClassModel>()
        for (input in transformInvocation.inputs) {
            input.directoryInputs.forEach(Consumer { dir: DirectoryInput ->
                val file = dir.file
                if (file.isDirectory) {
                    Files.walk(file.toPath()).filter { path: Path ->
                        val fileName = path.toFile().name
                        fileName.endsWith(".class") &&
                                !fileName.startsWith("R$") &&
                                "R.class" != fileName &&
                                "BuildConfig.class" != fileName
                    }.forEach { path: Path ->
                        val classFile = path.toFile()
                        try {
                            val decorateClassModel = findDecorateClass(classFile)
                            if (null != decorateClassModel) {
                                decorateClassModel.sourceFile = classFile
                                decorateClassList.add(decorateClassModel)
                            }
                        } catch (e: Exception) {
                            System.err.println("Process file:${classFile.name} failed.")
                        }
                    }
                }
            })
        }
        return decorateClassList
    }

    private fun findDecorateClass(classFile: File): DecorateClassModel? {
        val classReader = ClassReader(FileInputStream(classFile))
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val classVisitor = DecoratedClassVisitor(classWriter)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classVisitor.getDecorateClassModel()
    }

    private fun transformJarFiles(
        transformInvocation: TransformInvocation,
        decorateClassList: List<DecorateClassModel>
    ) {
        val outputProvider = transformInvocation.outputProvider
        //Copy all the jar and classes to the where they need to...
        for (input in transformInvocation.inputs) {
            input.jarInputs.forEach { jarInput: JarInput ->
                val dest = outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                if (dest.exists()) {
                    throw RuntimeException(
                        "Jar file " + jarInput.name + " already exists!" +
                                " src: " + jarInput.file.path + ", dest: " + dest.path
                    )
                }
                try {
                    processAndTransformJar(decorateClassList, jarInput.file, dest)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        }
    }

    private fun transformClassFiles(
        transformInvocation: TransformInvocation,
        decorateClassList: List<DecorateClassModel>
    ) {
        val outputProvider = transformInvocation.outputProvider
        for (input in transformInvocation.inputs) {
            input.directoryInputs.forEach(Consumer { dir: DirectoryInput ->
                val needDeleteClassList = mutableListOf<File?>()
                if (dir.file.isDirectory) {
                    dir.file.walk().forEach { classFile ->
                        val fileName = classFile.name
                        if (fileName.endsWith(".class") &&
                            !fileName.startsWith("R$") &&
                            "R.class" != fileName &&
                            "BuildConfig.class" != fileName
                        ) {
                            try {
                                if(classFile.name.contains('$')){
                                    //Inner class
                                    //We will delete the target inner class, then change the decorate inner class to replace the target inner class.
                                    val outerClassName = classFile.name.substringBefore("$")
                                    val outerFile = File(classFile.parentFile, "$outerClassName.class")
                                    val decorateClassModel = findDecorateClassModel(decorateClassList, outerFile.absolutePath)
                                    if (null != decorateClassModel) {
                                        println("\tProcess the inner class file:${classFile.name}")
                                        val classBytes = processInnerClassBytes(classFile,decorateClassModel)
                                        val targetSimpleClassName =
                                            decorateClassModel.targetClassName?.substringAfterLast(".")
                                        val innerClassName = classFile.name.substringAfter("$")
                                        println("\t$targetSimpleClassName$$innerClassName")
                                        File(classFile.parentFile, "$targetSimpleClassName$$innerClassName").writeBytes(classBytes)
                                        classFile.delete()
                                    }
                                } else {
                                    //Find the target class. Since we could use sourceFile to change the decorated class.
                                    val targetClassModel = findTargetClassModel(decorateClassList, classFile.absolutePath)
                                    if (null != targetClassModel) {
                                        needDeleteClassList.add(targetClassModel.sourceFile)
                                        println("\tProcess the source file:${classFile.absolutePath}")
                                        val classBytes = processClassBytes(targetClassModel)
                                        classFile.writeBytes(classBytes)
                                    }
                                }
                            } catch (e: Exception) {
                                System.err.println("Process file:${classFile.name} failed.")
                            }
                        }
                    }
                }
                try {
                    //Delete the class file before transform.
                    needDeleteClassList.forEach { file->
                        file?.delete()
                    }
                    val destFolder = outputProvider.getContentLocation(
                        dir.name,
                        dir.contentTypes,
                        dir.scopes,
                        Format.DIRECTORY
                    )
                    FileUtils.copyDirectory(dir.file, destFolder)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            })
        }
    }

    private inline fun findTargetClassModel(decorateClassList: List<DecorateClassModel>,classPath: String): DecorateClassModel?{
        return decorateClassList.find {
            val targetRelativeClassPath = it.targetRelativeClassPath
            if (null != targetRelativeClassPath)
                classPath.endsWith(targetRelativeClassPath)
            else
                false
        }
    }

    private inline fun findDecorateClassModel(decorateClassList: List<DecorateClassModel>,classPath: String): DecorateClassModel?{
        return decorateClassList.find {
            val targetRelativeClassPath = it.decorateRelativeClassPath
            if (null != targetRelativeClassPath)
                classPath.endsWith(targetRelativeClassPath)
            else
                false
        }
    }

    @Throws(IOException::class)
    private fun processAndTransformJar(decorateClassList: List<DecorateClassModel>, sourceFile: File, destFile: File) {
        if (null == sourceFile || !sourceFile.exists()) return
        val jarFile = JarFile(sourceFile)
        val newDestFile = File(destFile.parent, destFile.name.hashCode().toString() + destFile.name)
        val jarOutputStream = JarOutputStream(newDestFile.outputStream())

        val enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            val jarEntry = enumeration.nextElement() as JarEntry
            val inputStream = jarFile.getInputStream(jarEntry)
            val entryName = jarEntry.getName()
            jarOutputStream.putNextEntry(ZipEntry(entryName))

            var byteArray = inputStream.readBytes()
            val targetClassModel = findTargetClassModel(decorateClassList, entryName)
            if (null != targetClassModel) {
                byteArray = processClassBytes(targetClassModel)
            }
            jarOutputStream.write(byteArray)
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        jarFile.close()
        //Delete the old file and rename the new file.
        destFile.delete()
        newDestFile.renameTo(destFile)
    }

    private fun processInnerClassBytes(innerFile: File, decorateClassModel: DecorateClassModel): ByteArray {
        val decorateClassName = decorateClassModel.decorateClassName?.replace('.', '/')
        val targetClassName = decorateClassModel.targetClassName?.replace('.', '/')
        val classSet = collectClasses(decorateClassModel.sourceFile)
        val remapperMap = mutableMapOf<String, String>()
        classSet.forEach { className ->
            if (null != decorateClassName &&
                null != targetClassName &&
                className.contains(decorateClassName)
            ) {
                remapperMap[className] = className.replace(decorateClassName, targetClassName)
            }
        }
        val classReader = ClassReader(innerFile.readBytes())
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val simpleRemapper = SimpleRemapper(remapperMap)
        val classRemapper = ClassRemapper(classWriter, simpleRemapper)
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    private fun processClassBytes(decorateClassModel: DecorateClassModel): ByteArray {
        //Visit the code and check if is a test component.
        val sourceFile = decorateClassModel.sourceFile
        val decorateClassName = decorateClassModel.decorateClassName?.replace('.', '/')
        val targetClassName = decorateClassModel.targetClassName?.replace('.', '/')
        val classSet = collectClasses(sourceFile)
        val remapperMap = mutableMapOf<String, String>()
        classSet.forEach { className ->
            if (null != decorateClassName &&
                null != targetClassName &&
                className.contains(decorateClassName)
            ) {
                remapperMap[className] = className.replace(decorateClassName, targetClassName)
            }
        }
        val classReader = ClassReader(sourceFile?.readBytes())
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val simpleRemapper = SimpleRemapper(remapperMap)
        val classRemapper = object :ClassRemapper(classWriter, simpleRemapper){
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                if(true == name?.contains("\$lambda")){
                    println("method:$name")
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES)
        val file = File(project.buildDir, "file-tmp")
        if (!file.exists()) {
            file.mkdir()
        }
        val targetClassSimpleName = decorateClassModel.targetClassName?.substringAfterLast(".")
        File(file, "$targetClassSimpleName.class").writeBytes(classWriter.toByteArray())
        return classWriter.toByteArray()
    }

    private fun collectClasses(file: File?): Set<String> {
        if (null == file || !file.exists()) {
            return Collections.emptySet()
        }
        val classReader = ClassReader(file.readBytes())
        val cw = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        val classSet = HashSet<String>()
        val simpleRemapper = object : SimpleRemapper(null, null) {
            override fun mapType(internalName: String?): String {
                if (null != internalName) {
                    classSet.add(internalName);
                }
                return super.mapType(internalName)
            }
        }
        val remapper = ClassRemapper(cw, simpleRemapper)
        classReader.accept(remapper, ClassReader.EXPAND_FRAMES);
        return classSet
    }

}