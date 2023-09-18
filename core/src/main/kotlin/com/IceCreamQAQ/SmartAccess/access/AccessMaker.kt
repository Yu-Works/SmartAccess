package com.IceCreamQAQ.SmartAccess.access

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

object AccessMaker {

    private val Class<*>.internalName get() = Type.getInternalName(this)

    fun <T> make(dao: Class<T>, daoImplClass: Class<*>): ByteArray {
        val di = dao.internalName
        val dii = daoImplClass.internalName
        val cw = ClassWriter(0)
        cw.visit(
            V1_8,
            ACC_PUBLIC,
            "$di\$Impl",
            null,
            dii,
            arrayOf(di)
        )

        cw.visitEnd()
        return cw.toByteArray()
    }



}