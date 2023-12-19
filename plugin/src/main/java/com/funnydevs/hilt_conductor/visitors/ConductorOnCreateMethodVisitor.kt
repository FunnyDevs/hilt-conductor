package com.funnydevs.hilt_conductor.visitors


import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter

class ConductorOnCreateMethodVisitor(
    mv: MethodVisitor,
    descriptor: String,
    access: Int,
    private val controllerName: String,
) : LocalVariablesSorter(Opcodes.ASM7, access, descriptor, mv) {

    private val componentLifeCycleHandler = "com/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler"
    private var conductorInterfaceVarIndex = -1
    override fun visitCode() {
        super.visitCode()


        // Load 'this' and call getActivity()
        loadThisAndInvokeMethod("getActivity", "()Landroid/app/Activity;")

        // Get ConductorInterface instance
        getHiltEntryPoint()

        // Set 'handler' field
        setHandlerField()

        // Invoke 'handler.inject(getActivity(), this)'
        invokeHandlerInject()

        // Controller_Injecter.inject(handler, this)
        invokeControllerInject()

    }

    private fun invokeControllerInject() {
        mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
        mv.visitFieldInsn(Opcodes.GETFIELD,
            controllerName,
            "handler",
            "Lcom/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler;"
        )
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(INVOKESTATIC,
            "${controllerName}_Injector",
            "inject",
            "(Lcom/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler;L$controllerName;)V",
            false
        );
    }

    private fun loadThisAndInvokeMethod(methodName: String, methodDesc: String) {
        mv.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, controllerName, methodName, methodDesc, false)
    }

    private fun getHiltEntryPoint() {
        val hiltInterfaceTypeDesc = "Lcom/funnydevs/hilt_conductor/ConductorInterface;"
        val hiltInterfaceType = Type.getType(hiltInterfaceTypeDesc)
        conductorInterfaceVarIndex = newLocal(hiltInterfaceType)
        mv.visitLdcInsn(hiltInterfaceType)
        mv.visitMethodInsn(INVOKESTATIC, "dagger/hilt/EntryPoints", "get", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false)
        mv.visitTypeInsn(Opcodes.CHECKCAST, hiltInterfaceType.internalName)
        mv.visitVarInsn(Opcodes.ASTORE, conductorInterfaceVarIndex)
    }

    private fun setHandlerField() {
        mv.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
        mv.visitVarInsn(Opcodes.ALOAD, conductorInterfaceVarIndex)
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "com/funnydevs/hilt_conductor/ConductorInterface", "Conductor_LifeCycleHandler", "()L${componentLifeCycleHandler};", true)
        mv.visitFieldInsn(Opcodes.PUTFIELD, controllerName, "handler", "L${componentLifeCycleHandler};")
    }

    private fun invokeHandlerInject() {
        mv.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
        mv.visitFieldInsn(Opcodes.GETFIELD, controllerName, "handler", "L${componentLifeCycleHandler};")
        loadThisAndInvokeMethod("getActivity", "()Landroid/app/Activity;")
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, componentLifeCycleHandler, "inject", "(Landroid/app/Activity;Lcom/bluelinelabs/conductor/Controller;)V", false)
    }

}
