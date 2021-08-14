package jack.android.gradle.embedfunction.transform

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import jack.android.gradle.embedfunction.model.DecorateClassModel
import jack.android.gradle.embedfunction.visitor.DecoratedClassVisitor
import jack.android.gradle.embedfunction.visitor.MergeClassVisitor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class EmbedFunctionTransform(private val project:Project) : Transform() {

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
        if (transformInvocation.isIncremental) {
            throw UnsupportedOperationException("Unsupported incremental build!")
        }
        val decorateClassList = collectTheDecorateClasses(transformInvocation)
        transformJarFiles(transformInvocation,decorateClassList)
        transformClassFiles(transformInvocation,decorateClassList)
        //Delete all the decorate class.
        if(null != decorateClassList){
            decorateClassList.forEach {
                it.sourceFile?.delete()
            }
        }
    }

    private fun collectTheDecorateClasses(transformInvocation: TransformInvocation):List<DecorateClassModel> {
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
                            if(null != decorateClassModel){
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

    private fun findDecorateClass(classFile: File):DecorateClassModel? {
        val classReader = ClassReader(FileInputStream(classFile))
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val classVisitor = DecoratedClassVisitor(classWriter)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classVisitor.getDecorateClassModel()
    }

    private fun transformJarFiles(transformInvocation: TransformInvocation,decorateClassList: List<DecorateClassModel>){
        val outputProvider = transformInvocation.outputProvider
        outputProvider.deleteAll()
        //Copy all the jar and classes to the where they need to...
        for (input in transformInvocation.inputs) {
            input.jarInputs.parallelStream().forEach { jarInput: JarInput ->
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
                    processJarFile(decorateClassList,jarInput.file,dest)
                    FileUtils.copyFile(jarInput.file, dest)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        }
    }

    private fun transformClassFiles(transformInvocation: TransformInvocation, decorateClassList: List<DecorateClassModel>) {
        println("Start process all the classes file.")
        val outputProvider = transformInvocation.outputProvider
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
                            val targetClassModel = decorateClassList.find {
                                val targetRelativeClassPath = it.targetRelativeClassPath
                                if(null != targetRelativeClassPath)
                                    classFile.path.endsWith(targetRelativeClassPath)
                                else
                                    false
                            }
                            if(null != targetClassModel){
                                println("Process the source file:${classFile.absolutePath}")
                                val classBytes = processClassBytes(targetClassModel, classFile.readBytes())
                                classFile.writeBytes(classBytes)
                            }
                        } catch (e: Exception) {
                            System.err.println("Process file:${classFile.name} failed.")
                        }
                    }
                }
                try {
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

    @Throws(IOException::class)
    private fun processJarFile(decorateClassList: List<DecorateClassModel>,file: File, dest: File) {
        if(null == file||!file.exists()) return
        val jarFile = JarFile(file)
        val newDestFile = File(dest.parent,dest.name.hashCode().toString()+dest.name)
        val jarOutputStream = JarOutputStream(newDestFile.outputStream())
        val enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            val jarEntry = enumeration.nextElement() as JarEntry
            val inputStream = jarFile.getInputStream(jarEntry)
            val entryName = jarEntry.getName()
            val zipEntry = ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)

            var byteArray = inputStream.readBytes()
            val targetClassModel = decorateClassList.find {
                val targetRelativeClassPath = it.targetRelativeClassPath
                if(null != targetRelativeClassPath)
                    entryName.endsWith(targetRelativeClassPath)
                else
                    false
            }
            if(null != targetClassModel){
                println("Process the file in Jar:$entryName")
                byteArray = processClassBytes(targetClassModel,byteArray)
            }
            jarOutputStream.write(byteArray)
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        jarFile.close()
        //Delete the old file and rename the new file.
        dest.delete()
        newDestFile.renameTo(dest)
    }

    private fun processClassBytes(decorateClassModel:DecorateClassModel, byteArray: ByteArray):ByteArray {
        //Visit the code and check if is a test component.
        val sourceFile = decorateClassModel.sourceFile
        val sourceClassReader = ClassReader(sourceFile?.readBytes());
        val classNode = ClassNode()
        sourceClassReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        val targetClassReader = ClassReader(byteArray)
        val classWriter = ClassWriter(targetClassReader, ClassWriter.COMPUTE_FRAMES)
        val classVisitor = MergeClassVisitor(classNode,classWriter)
        targetClassReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

        val file = File(project.buildDir,"file-tmp")
        if(!file.exists()){
            file.mkdir()
        }
        val targetClassSimpleName = decorateClassModel.targetClassName?.substringAfterLast(".")
        File(file, "$targetClassSimpleName.class").writeBytes(classWriter.toByteArray())
        return classWriter.toByteArray()
    }

}