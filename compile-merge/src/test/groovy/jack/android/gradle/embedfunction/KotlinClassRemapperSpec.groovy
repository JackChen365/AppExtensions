package jack.android.gradle.embedfunction

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import spock.lang.Specification

class KotlinClassRemapperSpec extends Specification{
    def "test kotlin class remapper"(){
        given:
        def file = new File("src/test/assets/TestKotlinClasses/MainActivity_Decorate.class")
        String decorateClass = "jack/android/embedfunction/MainActivity_Decorate"
        String targetClass = "jack/android/embedfunction/MainActivity"
        def classSet = collectClasses(file)
        def map = new HashMap()
        classSet.each { className->
            if(className.contains(decorateClass)){
                map.put(className,className.replace(decorateClass,targetClass))
            }
        }
        def classReader = new ClassReader(file.bytes)
        ClassWriter classWriter = new ClassWriter(classReader,ClassWriter.COMPUTE_MAXS);
        ClassVisitor classRemapper = new ClassRemapper(classWriter, new SimpleRemapper(map))
        classReader.accept(classRemapper,ClassReader.EXPAND_FRAMES);

        File newFile = new File(file.parent,"newMainActivity.class")
        newFile.setBytes(classWriter.toByteArray())
        expect:
        newFile.exists()
    }

    private Set<String> collectClasses(File file){
        def classReader = new ClassReader(file.bytes)
        ClassWriter cw = new ClassWriter(classReader,ClassWriter.COMPUTE_MAXS);
        Set<String> set = new HashSet()
        def simpleRemapper = new SimpleRemapper(null,null){
            @Override
            String mapType(final String internalName) {
                set.add(internalName)
                return super.mapType(internalName)
            }
        }
        ClassVisitor remapper = new ClassRemapper(cw, simpleRemapper){
            @Override
            void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
                super.visitInnerClass(name, outerName, innerName, access)
            }

            @Override
            MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                if(name.contains("\$lambda")){
                    def visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
                    return new MethodVisitor(Opcodes.ASM5,visitMethod) {
                        @Override
                        void visitMethodInsn(final int opcode, final String owner, final String name1, final String descriptor1, final boolean isInterface) {
                            println "visitMethodInsn:$opcode owner:$owner name1:$name1 descriptor1:$descriptor1"
                            super.visitMethodInsn(opcode, owner, name1, descriptor1, isInterface)
                        }

                        @Override
                        void visitTypeInsn(final int opcode, final String type) {
                            println "visitTypeInsn:$opcode String:$type"
                            super.visitTypeInsn(opcode, type)
                        }
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }

            @Override
            void visitNestMember(final String nestMember) {
                super.visitNestMember(nestMember)
            }
        }
        classReader.accept(remapper,ClassReader.EXPAND_FRAMES);
        return set
    }
}
