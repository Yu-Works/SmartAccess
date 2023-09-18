package com.IceCreamQAQ.SmartAccess.jpa.access

import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.item.Page
import com.IceCreamQAQ.SmartAccess.jdbc.access.JDBCPageAble
import com.IceCreamQAQ.SmartAccess.jdbc.annotation.Execute
import com.IceCreamQAQ.SmartAccess.jpa.JPAService
import com.IceCreamQAQ.Yu.allMethod
import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.fullName
import com.IceCreamQAQ.Yu.hasAnnotation
import com.IceCreamQAQ.Yu.util.getLoad
import com.IceCreamQAQ.Yu.util.getTyped
import com.IceCreamQAQ.Yu.util.type.RelType
import com.IceCreamQAQ.Yu.util.visitIntInsn
import jakarta.persistence.Query
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.kotlinFunction

object JpaAccessMaker {

    private val Class<*>.internalName get() = Type.getInternalName(this)
    private val serviceOwner = JPAService::class.java.internalName
    private val pageableOwner = JDBCPageAble::class.java.internalName
    private val serviceDescriptor = Type.getDescriptor(JPAService::class.java)
    private val pageableDescriptor = Type.getDescriptor(JDBCPageAble::class.java)

    private val pageOwner = Page::class.java.internalName
    private val pageDescriptor = Type.getDescriptor(Page::class.java)

    private val queryOwner = Type.getInternalName(Query::class.java)
    private val queryDescriptor = Type.getDescriptor(Query::class.java)

    private val typedQueryOwner = Type.getInternalName(Query::class.java)
    private val typedQueryDescriptor = Type.getDescriptor(Query::class.java)

    private val baseMethods = JpaAccess::class.java.allMethod.map { it.name }


    private val stringDescriptor = Type.getDescriptor(String::class.java)
    private val classDescriptor = Type.getDescriptor(Class::class.java)

    operator fun invoke(
        implAccess: Class<*>,
        access: Class<Access<*, *>>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>
    ): ByteArray {
        val accessOwner = access.internalName
        val implOwner = implAccess.internalName

        val modelDescriptor = Type.getDescriptor(moduleType)
        val pkDescriptor = Type.getDescriptor(primaryKeyType)

        val cw = ClassWriter(0)
        cw.visit(
            V1_8,
            ACC_PUBLIC,
            "$accessOwner\$Impl",
            null,
            implOwner,
            arrayOf(accessOwner)
        )

        val isKotlin = access.hasAnnotation<Metadata>()
        val kAccess = access.kotlin

        // 构造函数
        apply {
            val constructorDescriptor =
                Type.getConstructorDescriptor(implAccess.kotlin.primaryConstructor!!.javaConstructor!!)

            cw.visitMethod(ACC_PUBLIC, "<init>", constructorDescriptor, null, null)
                .apply {
                    visitCode()
                    visitVarInsn(ALOAD, 0)
                    visitVarInsn(ALOAD, 1)
                    visitVarInsn(ALOAD, 2)
                    visitVarInsn(ALOAD, 3)
                    visitVarInsn(ALOAD, 4)
                    visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        implOwner,
                        "<init>",
                        constructorDescriptor,
                        false
                    )
                    visitInsn(Opcodes.RETURN)
                    visitMaxs(1, 1)
                    visitEnd()
                }
        }

        access.allMethod.filter { it.name !in baseMethods }
            .forEach { method ->
                val kFun = method.kotlinFunction

                val returnRelType = RelType.create(method.genericReturnType)
                if (returnRelType.realClass.isArray) error("方法 ${method.fullName} 返回值不能为数组！请使用 List 接受返回值！")
                val isList = List::class.java.isAssignableFrom(returnRelType.realClass)
                if (isList && returnRelType.realClass != List::class.java) error("方法 ${method.fullName} 只能接受 List 类型！不能接受 List 其子类型！")
                if (isList && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 List 时必须指定泛型类型！")

                val realType = if (isList) returnRelType.generics!![0].realClass else returnRelType.realClass
                val isModel = realType == moduleType

                val queryIndex = method.parameterCount

                fun MethodVisitor.makeExecute(query: String) {
                    visitVarInsn(ALOAD, 0)
                    visitLdcInsn(query)
                    visitMethodInsn(INVOKEVIRTUAL, implOwner, "jpaQuery", "($stringDescriptor)$queryDescriptor", false)
                    visitVarInsn(ASTORE, queryIndex)

                    method.parameters.forEachIndexed { i, it ->
                        visitVarInsn(ALOAD, queryIndex)
                        visitIntInsn(i)
                        it.type.name.let { type ->
                            visitVarInsn(getLoad(type), i + 1)
                            // 判断是否为基础数据类型，如果是基础数据类型则需要调用对应封装类型 valueOf 转换为封装类型。
                            if (type.length == 1)
                                getTyped(type).let { typed ->
                                    visitMethodInsn(INVOKEINTERFACE, typed, "valueOf", "($type)L$typed;", true)
                                }

                        }
                        visitMethodInsn(
                            INVOKEVIRTUAL,
                            queryOwner,
                            "setParameter",
                            "(ILjava/lang/Object;)L$queryOwner;",
                            false
                        )
                    }
                    visitVarInsn(ALOAD, queryIndex)
                    visitMethodInsn(INVOKEINTERFACE, queryOwner, "executeUpdate", "()I", true)
                    visitInsn(IRETURN)
                }

                fun MethodVisitor.makeSelect(query: String) {
                    if (!isModel) error("暂不支持非 Model 的返回值类型。")

                    val havePage = method.parameters.last().type == Page::class.java
                    val paramCount = if (havePage) method.parameterCount - 1 else method.parameterCount

                    visitVarInsn(ALOAD, 0)
                    visitLdcInsn(query)
                    visitMethodInsn(
                        INVOKEVIRTUAL,
                        implOwner,
                        "typedQuery",
                        "($stringDescriptor$classDescriptor)$typedQueryDescriptor",
                        false
                    )
                    visitVarInsn(ASTORE, queryIndex)

                    for (i in 0..<paramCount) {
                        visitVarInsn(ALOAD, queryIndex)
                        visitIntInsn(i)
                        method.parameters[i].type.name.let { type ->
                            visitVarInsn(getLoad(type), i + 1)
                            // 判断是否为基础数据类型，如果是基础数据类型则需要调用对应封装类型 valueOf 转换为封装类型。
                            if (type.length == 1)
                                getTyped(type).let { typed ->
                                    visitMethodInsn(INVOKEINTERFACE, typed, "valueOf", "($type)L$typed;", true)
                                }

                        }
                        visitMethodInsn(
                            INVOKEINTERFACE,
                            typedQueryOwner,
                            "setParameter",
                            "(ILjava/lang/Object;)L$typedQueryDescriptor;",
                            true
                        )
                    }

                    if (havePage) {
                        val pageIndex = queryIndex - 1
                        visitVarInsn(ALOAD, queryIndex)
                        visitVarInsn(ALOAD, pageIndex)
                        visitMethodInsn(INVOKEVIRTUAL, pageOwner, "getStart", "()I", false)
                        visitMethodInsn(
                            INVOKEINTERFACE,
                            typedQueryOwner,
                            "setFirstResult",
                            "(I)$typedQueryDescriptor",
                            true
                        )
                        visitVarInsn(ALOAD, queryIndex)
                        visitVarInsn(ALOAD, pageIndex)
                        visitMethodInsn(INVOKEVIRTUAL, pageOwner, "getNum", "()I", false)
                        visitMethodInsn(
                            INVOKEINTERFACE,
                            typedQueryOwner,
                            "setMaxResults",
                            "(I)$typedQueryDescriptor",
                            true
                        )
                    }

                }

                method.annotation<Execute> { }
            }



        cw.visitEnd()
        return cw.toByteArray()
    }


}