package com.IceCreamQAQ.SmartAccess.jpa.access

import com.IceCreamQAQ.SmartAccess.ServiceAccessMaker
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.classDescriptor
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.descriptor
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.internalName
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.listDescriptor
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.objectDescriptor
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.pageOwner
import com.IceCreamQAQ.SmartAccess.access.AccessMaker.stringDescriptor
import com.IceCreamQAQ.SmartAccess.access.query.AbstractQuery
import com.IceCreamQAQ.SmartAccess.item.Page
import com.IceCreamQAQ.SmartAccess.jdbc.annotation.Execute
import com.IceCreamQAQ.SmartAccess.jdbc.annotation.Select
import com.IceCreamQAQ.Yu.allMethod
import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.util.*
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import java.lang.reflect.Method

object JpaAccessMaker : ServiceAccessMaker {

    override val baseMethods = JpaAccess::class.java.allMethod.map { it.name }


    private val queryOwner = Query::class.java.internalName
    private val queryDescriptor = Query::class.java.descriptor

    private val typedQueryOwner = TypedQuery::class.java.internalName
    private val typedQueryDescriptor = TypedQuery::class.java.descriptor

    override fun MethodVisitor.makeConstructor(
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        constructorDescriptor: String
    ) {
        visitCode()
        visitVarInsn(ALOAD, 0)
        visitVarInsn(ALOAD, 1)
        visitVarInsn(ALOAD, 2)
        visitVarInsn(ALOAD, 3)
        visitVarInsn(ALOAD, 4)
        visitMethodInsn(
            INVOKESPECIAL,
            implAccess.internalName,
            "<init>",
            constructorDescriptor,
            false
        )
        visitInsn(RETURN)
        visitMaxs(5, 5)
        visitEnd()
    }

    override fun MethodVisitor.makeSelect(
        method: Method,
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        query: String,
        isList: Boolean,
        isModel: Boolean,
        realType: Class<*>
    ) {
        val queryIndex = method.parameterCount + 1
        val returnTypeDescription = method.returnType.descriptor

//        if (!isModel) error("暂不支持非 Model 的返回值类型。")

        val (queryFunOwner, queryFunDescriptor) =
            if (isModel) typedQueryOwner to typedQueryDescriptor
            else queryOwner to queryDescriptor

        val havePage = method.parameters.last().type == Page::class.java
        val paramCount = if (havePage) method.parameterCount - 1 else method.parameterCount

        visitVarInsn(ALOAD, 0)
        visitLdcInsn(query)
        if (isModel) {
            visitLdcInsn(Type.getType(moduleType))
            visitMethodInsn(
                INVOKEVIRTUAL,
                implAccess.internalName,
                "typedQuery",
                "($stringDescriptor$classDescriptor)$queryFunDescriptor",
                false
            )
        } else visitMethodInsn(
            INVOKEVIRTUAL,
            implAccess.internalName,
            "jpaQuery",
            "($stringDescriptor)$queryFunDescriptor",
            false
        )
        visitVarInsn(ASTORE, queryIndex)

        for (i in 0..<paramCount) {
            visitVarInsn(ALOAD, queryIndex)
            visitIntInsn(i)
            Type.getDescriptor(method.parameters[i].type).let { type ->
                visitVarInsn(getLoad(type), i + 1)
                // 判断是否为基础数据类型，如果是基础数据类型则需要调用对应封装类型 valueOf 转换为封装类型。
                if (type.length == 1)
                    getTyped(type).let { typed ->
                        visitMethodInsn(INVOKESTATIC, typed, "valueOf", "($type)L$typed;", false)
                    }

            }
            visitMethodInsn(
                INVOKEINTERFACE,
                queryFunOwner,
                "setParameter",
                "(ILjava/lang/Object;)$queryFunDescriptor",
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
                queryFunOwner,
                "setFirstResult",
                "(I)$queryFunDescriptor",
                true
            )
            visitVarInsn(ALOAD, queryIndex)
            visitVarInsn(ALOAD, pageIndex)
            visitMethodInsn(INVOKEVIRTUAL, pageOwner, "getNum", "()I", false)
            visitMethodInsn(
                INVOKEINTERFACE,
                queryFunOwner,
                "setMaxResults",
                "(I)$queryFunDescriptor",
                true
            )
        }

        visitVarInsn(ALOAD, queryIndex)
        if (isList)
            visitMethodInsn(INVOKEINTERFACE, queryOwner, "getResultList", "()$listDescriptor", true)
        else
            visitMethodInsn(INVOKEINTERFACE, queryOwner, "getSingleResult", "()$objectDescriptor", true)
        makeCast(this, returnTypeDescription)
        visitInsn(getReturn(returnTypeDescription))

        visitMaxs(queryIndex + 1, 5)
        visitEnd()
    }

    override fun MethodVisitor.makeExecute(
        method: Method,
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        query: String
    ) {
        val queryIndex = method.parameterCount + 1

        visitVarInsn(ALOAD, 0)
        visitLdcInsn(query)
        visitMethodInsn(
            INVOKEVIRTUAL,
            implAccess.internalName,
            "jpaQuery",
            "($stringDescriptor)$queryDescriptor",
            false
        )
        visitVarInsn(ASTORE, queryIndex)

        method.parameters.forEachIndexed { i, it ->
            visitVarInsn(ALOAD, queryIndex)
            visitIntInsn(i)
            it.type.name.let { type ->
                visitVarInsn(getLoad(type), i + 1)
                // 判断是否为基础数据类型，如果是基础数据类型则需要调用对应封装类型 valueOf 转换为封装类型。
                if (type.length == 1)
                    getTyped(type).let { typed ->
                        visitMethodInsn(INVOKESTATIC, typed, "valueOf", "($type)L$typed;", false)
                    }

            }
            visitMethodInsn(
                INVOKEINTERFACE,
                queryOwner,
                "setParameter",
                "(ILjava/lang/Object;)$queryOwner",
                true
            )
        }
        visitVarInsn(ALOAD, queryIndex)
        visitMethodInsn(INVOKEINTERFACE, queryOwner, "executeUpdate", "()I", true)
        visitInsn(IRETURN)

        visitMaxs(queryIndex + 1, 4)
        visitEnd()
    }

    override fun AbstractQuery.serialize(moduleType: Class<*>): String =
        toSqlString(moduleType.simpleName, needSelect = false, needIndex = true)

    override fun Method.haveSelect(): String? = annotation<Select>()?.value

    override fun Method.haveExecute(): String? = annotation<Execute>()?.value

}