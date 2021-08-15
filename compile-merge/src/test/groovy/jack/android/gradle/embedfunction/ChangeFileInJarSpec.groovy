package jack.android.gradle.embedfunction

import jdk.internal.org.objectweb.asm.Opcodes
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.spockframework.runtime.InvalidSpecException
import spock.lang.Specification

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
class ChangeFileInJarSpec extends Specification{

    def "test change classes file in Jar"(){
        given:
        def file = new File("src/test/assets/TestJars/embedfunction.jar")
        def newFile = modifyJar(file)
        when:
        checkClassFileFromJar(newFile)
        then:
        noExceptionThrown()
    }

    private File modifyJar(File file) {
        if(null == file||!file.exists()) return
        JarFile jarFile = new JarFile(file)
        File outputJar = new File(file.parent,"new-"+file.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = jarFile.getInputStream(jarEntry)
            String entryName = jarEntry.getName()
            ZipEntry zipEntry = new ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)
            if (entryName.endsWith("EmbedFunctionPlugin.class")) {
                byte[] changedClassBytes = processClassBytes(inputStream.bytes)
                jarOutputStream.write(changedClassBytes)
            } else {
                jarOutputStream.write(IOUtils.toByteArray(inputStream))
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        jarFile.close()
        return outputJar
    }

    private byte[] processClassBytes(byte[] classBytes) {
        //Visit the code and check if is a test component.
        def classReader = new ClassReader(classBytes)
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        def classVisitor = new ChangeMethodClassVisitor(classWriter)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    private void checkClassFileFromJar(File file) throws InvalidSpecException{
        if(null == file||!file.exists()) return
        JarFile jarFile = null
        try{
            jarFile = new JarFile(file)
            Enumeration enumEntries = jarFile.entries()
            while (enumEntries.hasMoreElements()) {
                def jarEntry = (JarEntry) enumEntries.nextElement()
                if(jarEntry.name.endsWith("EmbedFunctionPlugin.class")){
                    InputStream inputStream = jarFile.getInputStream(jarEntry)
                    byte[] classBytes = IOUtils.toByteArray(inputStream)
                    def classReader = new ClassReader(classBytes)
                    def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    def classVisitor = new MethodCheckClassVisitor(classWriter)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                }
            }
        } finally{
            jarFile?.close()
        }
    }

    static class ChangeMethodClassVisitor extends ClassVisitor{


        ChangeMethodClassVisitor(final ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor)
        }

        @Override
        MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            if(name == "configureTempSourceFolder"){
                return null
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    static class MethodCheckClassVisitor extends ClassVisitor{

        MethodCheckClassVisitor(final ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor)
        }

        @Override
        MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            if(name == "configureTempSourceFolder"){
                thrown()
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
}
