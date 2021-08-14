package jack.android.gradle.embedfunction.visitor;

import org.objectweb.asm.*
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class MergeClassVisitor(
    private val classNode: ClassNode,
    cv: ClassVisitor
) : ClassVisitor(Opcodes.ASM5, cv) {
    private lateinit var className: String;

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        this.className = name;
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if ("Ljack/android/embedfunction/api/Decorate;" == descriptor) {
            return null;
        }
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        if (classNode.fields.any { it.name == name && it.desc == descriptor }) {
            return null
        }
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        if (classNode.methods.any { it.name == name && it.desc == descriptor }) {
            return null
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    override fun visitEnd() {
//        val invisibleAnnotationIterator = classNode.invisibleAnnotations?.iterator()
//        while (null != invisibleAnnotationIterator && invisibleAnnotationIterator.hasNext()) {
//            val annotationNode = invisibleAnnotationIterator.next() as AnnotationNode
//            if ("Ljack/android/embedfunction/api/Decorate;" != annotationNode.desc) {
//                annotationNode.accept(cv.visitAnnotation(annotationNode.desc, false))
//            }
//        }
//        val visibleAnnotationIterator = classNode.visibleAnnotations?.iterator()
//        while (null != visibleAnnotationIterator && visibleAnnotationIterator.hasNext()) {
//            val annotationNode = visibleAnnotationIterator.next() as AnnotationNode
//            if ("Ljack/android/embedfunction/api/Decorate;" != annotationNode.desc) {
//                annotationNode.accept(cv.visitAnnotation(annotationNode.desc, true))
//            }
//        }
        val fieldIterator = classNode.fields?.iterator()
        while (null != fieldIterator && fieldIterator.hasNext()) {
            val fieldNode = fieldIterator.next() as FieldNode
            fieldNode.accept(cv);
        }
        val methodIterator = classNode.methods?.iterator()
        while (null != methodIterator && methodIterator.hasNext()) {
            val methodNode = methodIterator.next() as MethodNode
            val methodVisitor = cv.visitMethod(
                methodNode.access, methodNode.name, methodNode.desc,
                methodNode.signature, methodNode.exceptions.toTypedArray()
            )
            methodNode.instructions.resetLabels();
            methodNode.accept(MethodRemapper(methodVisitor, SimpleRemapper(className, classNode.name)));
        }
        super.visitEnd()
    }
}