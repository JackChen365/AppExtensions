package jack.android.gradle.embedfunction

import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import spock.lang.Specification;

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
class MergeClassesSpec extends Specification {

    def "test replace method by using ASM"(){
        given:
        def decorateFile = new File("src/test/assets/TestClasses/DecorateClass.class")
        def targetFile = new File("src/test/assets/TestClasses/TargetClass.class")
        when:
        ClassReader cr = new ClassReader(IOUtils.toByteArray(new FileInputStream(decorateFile)));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        def classReader = new ClassReader(IOUtils.toByteArray(new FileInputStream(targetFile)))
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        def classVisitor = new RemapperClassVisitor(classWriter,cn)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

        File newFile = new File(targetFile.parentFile,"MergeClass.class")
        newFile.withOutputStream { outputStream->
            outputStream.write(classWriter.toByteArray())
        }
        then:
        noExceptionThrown()
    }

    class RemapperClassVisitor extends ClassVisitor {
        private ClassNode cn;
        private String cname;
        public RemapperClassVisitor(ClassVisitor cv,
                                    ClassNode cn) {
            super(Opcodes.ASM5,cv);
            this.cn = cn;
        }
        public void visit(int version, int access,
                          String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name,
                    signature, superName, interfaces);
            this.cname = name;
        }
        public void visitEnd() {
            for(Iterator it = cn.fields.iterator();
                it.hasNext();) {
                ((FieldNode) it.next()).accept(this);
            }
            for(Iterator it = cn.methods.iterator(); it.hasNext();) {
                MethodNode mn = (MethodNode) it.next();
                String[] exceptions = new String[mn.exceptions.size()];
                mn.exceptions.toArray(exceptions);
                MethodVisitor mv =
                        cv.visitMethod(
                                mn.access, mn.name, mn.desc,
                                mn.signature, exceptions);
                mn.instructions.resetLabels();
                mn.accept(new MethodRemapper(mv, new SimpleRemapper(cname, cn.name)));
            }
            super.visitEnd();
        }
    }
}
