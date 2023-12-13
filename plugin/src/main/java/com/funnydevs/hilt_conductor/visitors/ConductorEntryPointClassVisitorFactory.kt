package com.funnydevs.hilt_conductor.visitors

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * ASM Adapter that transforms @ConductorEntryPoint-annotated classes to extend the Hilt
 * generated android class, including the @HiltAndroidApp application class.
 */
class ConductorEntryPointClassVisitor(
    private val apiVersion: Int,
    nextClassVisitor: ClassVisitor,
) : ClassVisitor(apiVersion, nextClassVisitor) {


    abstract class Factory : AsmClassVisitorFactory<InstrumentationParameters.None> {
        override fun createClassVisitor(
            classContext: ClassContext,
            nextClassVisitor: ClassVisitor
        ): ClassVisitor {
            // val writer = PrintWriter(System.out) // or a file writer for output to a file
            // val traceVisitor = TraceClassVisitor(nextClassVisitor, writer)

            return ConductorEntryPointClassVisitor(
                apiVersion = instrumentationContext.apiVersion.get(),
                nextClassVisitor = nextClassVisitor,
            )
        }

        /**
         * Check if a class should be transformed.
         *
         * Only classes that are a Conductor entry point should be transformed.
         */
        override fun isInstrumentable(classData: ClassData): Boolean {
            return classData.classAnnotations.any { CONDUCTOR_ENTRY_POINT_ANNOTATIONS.contains(it) }
        }
    }


    private var isConductorEntryPoint = false
    private lateinit var controllerName:String
    private var controllerSuperName: String? = null

    // where the key is the field name and the value is the field descriptor
    private val injectableFields = mutableMapOf<String, String>()
    private var onDestroyViewExists = false


    override fun visit(version: Int,
                       access: Int,
                       name: String,
                       signature: String?,
                       superName: String?,
                       interfaces: Array<String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        this.controllerName = name
        this.controllerSuperName = superName

        // Add a field
        cv.visitField(
            /* access = */ Opcodes.ACC_PRIVATE,
            /* name = */ "handler",
            /* descriptor = */ "Lcom/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler;",
            /* signature = */ null,
            /* value = */ null
        )?.visitEnd()

    }

    override fun visitField(access: Int,
                            name: String,
                            desc: String,
                            signature: String?,
                            value: Any?): FieldVisitor {
        val fv = super.visitField(access, name, desc, signature, value)
        println("field name = $name")
        return object : FieldVisitor(Opcodes.ASM7, fv) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                if (descriptor == "Ljavax/inject/Inject;") {
                    injectableFields[name] = desc
                }
                return super.visitAnnotation(descriptor, visible)
            }
        }
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        println("visitAnnotation name = $descriptor")
        if (CONDUCTOR_ENTRY_POINT_ANNOTATIONS.contains(descriptor)) {
            isConductorEntryPoint = true
        }
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitMethod(
        access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?
    ): MethodVisitor {
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)
        println("visitMethod name = $name")

        // Check if this is the 'onCreateView' method.
        if (name == "onCreateView") {
            return ConductorOnCreateMethodVisitor(mv,desc,access, controllerName)
        }

        if (name == "onDestroyView") {
            // Modify the onDestroy method
            onDestroyViewExists = true
            return ConductorOnDestroyMethodVisitor(mv, controllerName)
        }
        return mv
    }

    override fun visitEnd() {
        if (!onDestroyViewExists) {
            // Create the onDestroyView method if it doesn't exist
            createOnDestroyViewMethod()
        }
        super.visitEnd()
    }

    private fun createOnDestroyViewMethod() {
        val mv = cv.visitMethod(Opcodes.ACC_PROTECTED,
            "onDestroyView",
            "(Landroid/view/View;)V",
            null,
            null
        )
        mv.visitCode()

        // Equivalent to: super.onDestroyView(view);
        // Load 'this' onto the stack
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        // Load the first method argument (the View parameter) onto the stack
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        // Invoke the super class's onDestroyView method
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
            controllerSuperName,
            "onDestroyView",
            "(Landroid/view/View;)V",
            false
        )

        // Equivalent to: this.handler.destroy();
        // Load 'this' onto the stack
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        // Get the 'handler' field from 'this'
        mv.visitFieldInsn(Opcodes.GETFIELD,
            controllerName,
            "handler",
            "Lcom/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler;"
        )
        // Invoke the 'destroy' method on 'handler'
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
            "com/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler",
            "destroy",
            "()V",
            false
        )

        // Return
        mv.visitInsn(Opcodes.RETURN)

        // Compute the maximum stack size and local variables automatically
        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    companion object {
        val CONDUCTOR_ENTRY_POINT_ANNOTATIONS = setOf(
            "com.funnydevs.hilt_conductor.annotations.ConductorEntryPoint",
            "Lcom/funnydevs/hilt_conductor/annotations/ConductorEntryPoint"
        )
    }
}
