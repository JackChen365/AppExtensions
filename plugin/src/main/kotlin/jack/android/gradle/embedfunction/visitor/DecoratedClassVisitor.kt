package jack.android.gradle.embedfunction.visitor;

import jack.android.gradle.embedfunction.model.DecorateClassModel
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File

class DecoratedClassVisitor(classVisitor: ClassVisitor?) : ClassVisitor(Opcodes.ASM5, classVisitor) {
    private var decorateClassModel: DecorateClassModel? = null
    private var decoratedClassName: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        decoratedClassName = name.replace('/', '.');
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        val annotationVisitor = super.visitAnnotation(descriptor, visible)
        if ("Ljack/android/embedfunction/api/Decorate;" == descriptor) {
            return object : AnnotationVisitor(Opcodes.ASM5, annotationVisitor) {
                override fun visit(name: String, value: Any?) {
                    super.visit(name, value)
                    ensureDecoratedClassModel()
                    if ("value" == name) {
                        decorateClassModel?.targetClassName = value.toString()
                    } else if ("target" == name) {
                        val targetClassDesc = value.toString()
                        decorateClassModel?.targetClassName =
                                targetClassDesc.replace('/', '.').substring(1, targetClassDesc.length - 1)
                    }
                    val targetClassName = decorateClassModel?.targetClassName
                    decorateClassModel?.targetRelativeClassPath = targetClassName?.replace('.','/')+".class"
                }
            }
        }
        return annotationVisitor
    }

    private fun ensureDecoratedClassModel() {
        if (null == decorateClassModel) {
            decorateClassModel = DecorateClassModel()
            decorateClassModel?.decorateClassName = decoratedClassName
            decorateClassModel?.decorateRelativeClassPath = decoratedClassName?.replace('.','/')+".class"
        }
    }

    fun getDecorateClassModel(): DecorateClassModel? {
        return decorateClassModel
    }
}