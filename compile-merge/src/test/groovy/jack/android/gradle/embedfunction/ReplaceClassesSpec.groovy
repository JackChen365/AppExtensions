package jack.android.gradle.embedfunction

import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import spock.lang.Specification

/**
 * Created on 2021/8/7.
 *
 * @author Jack Chen
 * @email bingo110@126.com
 */
class ReplaceClassesSpec extends Specification {

    def "test find decorate class from file"(){
        given:
        def file1 = new File("src/test/assets/TestClasses/TargetClass.class")
        def file2 = new File("src/test/assets/TestClasses/DecorateClass.class")
        expect:
        null == findDecorateClass(file1)
        null != findDecorateClass(file2)
    }

    def "test collect the decorated class"(){
        given:
        def fileList = new ArrayList()
        fileList<<new File("src/test/assets/TestClasses/TargetClass.class")
        fileList<<new File("src/test/assets/TestClasses/DecorateClass.class")
        expect:
        def decorateClassModelList = new ArrayList()
        for(def file: fileList){
            def decorateClassModel = findDecorateClass(file)
            if(null != decorateClassModel){
                decorateClassModelList.add(decorateClassModel)
            }
        }
        !decorateClassModelList.isEmpty()
    }

    def "test merge class change to source class"(){
        given:
        DecorateClassModel decorateClassModel = new DecorateClassModel()
        decorateClassModel.decoratedClassName="jack.android.gradle.embedfunction.asm.DecorateClass"
        decorateClassModel.targetClassName="jack.android.gradle.embedfunction.asm.TargetClass"
        decorateClassModel.sourceFile = new File("src/test/assets/TestClasses/DecorateClass.class")
        decorateClassModel.targetFile = new File("src/test/assets/TestClasses/TargetClass.class")
        expect:
        processSourceFile(decorateClassModel).exists()
    }

    private DecorateClassModel findDecorateClass(File file){
        def sourceFolder = new File("src/assets/TestClasses")
        def classReader = new ClassReader(IOUtils.toByteArray(new FileInputStream(file)))
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        def classVisitor = new DecoratedClassVisitor(sourceFolder,classWriter)
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classVisitor.getDecorateClassModel()
    }

    private File processSourceFile(DecorateClassModel decorateClassModel){
        def sourceFile = decorateClassModel.sourceFile
        ClassReader sourceClassReader = new ClassReader(IOUtils.toByteArray(new FileInputStream(sourceFile)));
        ClassNode classNode = new ClassNode();
        sourceClassReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        def targetFile = decorateClassModel.targetFile
        def targetClassReader = new ClassReader(IOUtils.toByteArray(new FileInputStream(targetFile)))
        def classWriter = new ClassWriter(targetClassReader, ClassWriter.COMPUTE_FRAMES)
        def classVisitor = new MergeClassVisitor(classWriter,classNode,decorateClassModel)
        targetClassReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

        File newFile = new File(targetFile.parentFile,"New"+targetFile.name)
        newFile.withOutputStream { outputStream->
            outputStream.write(classWriter.toByteArray())
        }
        targetClassReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return newFile
    }

    class DecorateClassModel{
        private File sourceFile;
        private File targetFile;
        private String decoratedClassName;
        private String targetClassName;
    }

    class DecoratedClassVisitor extends ClassVisitor {
        private DecorateClassModel decorateClassModel
        private File sourceFolder;
        private String decoratedClassName;
        public DecoratedClassVisitor(File file,ClassVisitor cv) {
            super(Opcodes.ASM5,cv);
            this.sourceFolder = file;
        }
        public void visit(int version, int access,
                          String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name,
                    signature, superName, interfaces);
            decoratedClassName = name.replace('/','.');
        }

        @Override
        AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
            def annotationVisitor= super.visitAnnotation(descriptor, visible)
            if("Ljack/android/embedfunction/api/Decorate;" == descriptor){
                return new AnnotationVisitor(Opcodes.ASM5,annotationVisitor) {
                    @Override
                    void visit(final String name, final Object value) {
                        super.visit(name, value)
                        ensureDecoratedClassModel()
                        if("value" == name){
                            decorateClassModel.targetClassName = String.valueOf(value)
                        } else if("target" == name){
                            def targetClassDesc = String.valueOf(value)
                            decorateClassModel.targetClassName = targetClassDesc.replace('/','.').substring(1,targetClassDesc.length()-1)
                        }
                        def targetClassName = decorateClassModel.targetClassName
                        decorateClassModel.targetFile = new File(sourceFolder,targetClassName.replace('.','/')+".class")
                    }
                }
            }
            return annotationVisitor
        }

        @Override
        MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        private void ensureDecoratedClassModel(){
            if(null == decorateClassModel){
                decorateClassModel = new DecorateClassModel()
                decorateClassModel.decoratedClassName = decoratedClassName
                decorateClassModel.sourceFile = new File(sourceFolder,decoratedClassName.replace('.','/')+".class")
            }
        }

        DecorateClassModel getDecorateClassModel() {
            return decorateClassModel
        }
    }

    class MergeClassVisitor extends ClassVisitor {
        private DecorateClassModel decorateClassModel;
        private ClassNode classNode;
        private String className;
        public MergeClassVisitor(ClassVisitor cv,
                                 ClassNode classNode,
                                 DecorateClassModel decorateClassModel) {
            super(Opcodes.ASM5,cv);
            this.classNode = classNode;
            this.decorateClassModel = decorateClassModel
        }
        public void visit(int version, int access,
                          String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name,
                    signature, superName, interfaces);
            this.className = name;
        }

        @Override
        AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
            if("Ljack/android/embedfunction/api/Decorate;" == descriptor){
                return null;
            }
            return super.visitAnnotation(descriptor, visible)
        }

        @Override
        FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
            if(classNode.fields.find {it.name == name && it.desc == descriptor}){
                return null
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        @Override
        MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            if(classNode.methods.find { it.name == name && it.desc == descriptor }){
                return null
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        public void visitEnd() {
            def invisibleAnnotationIterator = classNode.invisibleAnnotations.iterator()
            while(invisibleAnnotationIterator.hasNext()){
                def annotationNode = (AnnotationNode) invisibleAnnotationIterator.next()
                if("Ljack/android/embedfunction/api/Decorate;" != annotationNode.desc) {
                    annotationNode.accept(cv.visitAnnotation(annotationNode.desc, false))
                }
            }
            def visibleAnnotationIterator = classNode.visibleAnnotations.iterator()
            while(visibleAnnotationIterator.hasNext()){
                def annotationNode = (AnnotationNode) visibleAnnotationIterator.next()
                if("Ljack/android/embedfunction/api/Decorate;" != annotationNode.desc) {
                    annotationNode.accept(cv.visitAnnotation(annotationNode.desc,true))
                }
            }
            def fieldIterator = classNode.fields.iterator()
            while(fieldIterator.hasNext()) {
                def fieldNode= (FieldNode) fieldIterator.next();
                fieldNode.accept(cv);
            }
            def methodIterator = classNode.methods.iterator()
            while(methodIterator.hasNext()){
                MethodNode methodNode = (MethodNode) methodIterator.next()
                String[] exceptions = new String[methodNode.exceptions.size()];
                methodNode.exceptions.toArray(exceptions);
                MethodVisitor methodVisitor =
                        cv.visitMethod(
                                methodNode.access, methodNode.name, methodNode.desc,
                                methodNode.signature, exceptions);
                methodNode.instructions.resetLabels();
                methodNode.accept(new MethodRemapper(methodVisitor, new SimpleRemapper(className, classNode.name)));
            }
            super.visitEnd();
        }
    }
}
