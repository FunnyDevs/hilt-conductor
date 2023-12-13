package com.funnydevs.hilt_conductor.visitors

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ConductorOnDestroyMethodVisitor(mv: MethodVisitor,
    private val controllerName: String)
    : MethodVisitor(Opcodes.ASM7, mv) {

    override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN) {
            // Inject the handler.destroy() call just before every RETURN instruction
            mv.visitVarInsn(Opcodes.ALOAD, 0) // Load 'this'
            mv.visitFieldInsn(Opcodes.GETFIELD, controllerName, "handler", "Lcom/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler;")
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/funnydevs/hilt_conductor/ConductorComponentLifecycleHandler", "destroy", "()V", false)
        }
        super.visitInsn(opcode)
    }


}
