package smartaccess.jpa.access

import jakarta.persistence.LockModeType
import smartaccess.ServiceAccessMaker
import smartaccess.access.AccessMaker.classDescriptor
import smartaccess.access.AccessMaker.descriptor
import smartaccess.access.AccessMaker.internalName
import smartaccess.access.AccessMaker.listDescriptor
import smartaccess.access.AccessMaker.objectDescriptor
import smartaccess.access.AccessMaker.pageOwner
import smartaccess.access.AccessMaker.stringDescriptor
import smartaccess.access.query.AbstractQuery
import smartaccess.item.Page
import smartaccess.jdbc.annotation.Execute
import smartaccess.jdbc.annotation.Select
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import rain.function.*
import smartaccess.access.AccessMaker.objectOwner
import smartaccess.access.AccessMaker.pageDescriptor
import smartaccess.access.AccessMaker.pageResultDescriptor
import smartaccess.jpa.annotation.Lock
import java.lang.reflect.Method

object JpaAccessMaker : ServiceAccessMaker {

    override val baseMethods = JpaAccess::class.java.allMethod.map { it.name }


    private val queryOwner = Query::class.java.internalName
    private val queryDescriptor = Query::class.java.descriptor

    private val typedQueryOwner = TypedQuery::class.java.internalName
    private val typedQueryDescriptor = TypedQuery::class.java.descriptor

    private val lockModeOwner = LockModeType::class.java.internalName
    private val lockModeDescriptor = LockModeType::class.java.descriptor

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
        isPage: Boolean,
        isModel: Boolean,
        realType: Class<*>
    ) {
        val params = run {
            var num = 1
            method.parameters.map {
                val size = when (it.type) {
                    Long::class.javaPrimitiveType, Double::class.javaPrimitiveType -> 2
                    else -> 1
                }
                MethodPara(size, num, it.type.descriptor).also { num += size }
            }
        }
        val methodStack = (params.lastOrNull()?.let { it.stackNum + it.stackSize } ?: 0)
        val hasWidth = params.any { it.stackSize == 2 }

        val lock = method.getAnnotation(Lock::class.java)?.value?.name

        val queryIndex = methodStack
        val returnTypeDescription = method.returnType.descriptor

//        if (!isModel) error("暂不支持非 Model 的返回值类型。")

        val (queryFunOwner, queryFunDescriptor) =
            if (isModel) typedQueryOwner to typedQueryDescriptor
            else queryOwner to queryDescriptor

        val havePage = method.parameters.last().type == Page::class.java
        val paramCount = if (havePage) method.parameterCount - 1 else method.parameterCount

        visitVarInsn(ALOAD, 0)
        visitLdcInsn(query)
        if (isPage) {
            if (lock != null) visitFieldInsn(GETSTATIC, lockModeOwner, lock, lockModeDescriptor)
            visitVarInsn(ALOAD, method.parameterCount)

            visitIntInsn(paramCount)
            visitTypeInsn(ANEWARRAY, objectOwner)

            repeat(paramCount) { i ->
                val it = params[i]

                visitInsn(DUP)
                visitIntInsn(i)
                it.type.let { type ->
                    visitVarInsn(getLoad(type), it.stackNum)
                    // 判断是否为基础数据类型，如果是基础数据类型则需要调用对应封装类型 valueOf 转换为封装类型。
                    if (type.length == 1)
                        getTyped(type).let { typed ->
                            visitMethodInsn(INVOKESTATIC, typed, "valueOf", "($type)L$typed;", false)
                        }
                }
                visitInsn(AASTORE)
            }
            visitMethodInsn(
                INVOKEVIRTUAL,
                implAccess.internalName,
                "page",
                if (lock != null) "($stringDescriptor$lockModeDescriptor$pageDescriptor[$objectDescriptor)$pageResultDescriptor"
                else "($stringDescriptor$pageDescriptor[$objectDescriptor)$pageResultDescriptor",
                false
            )
            visitInsn(ARETURN)
            var maxStack = 7
            if (hasWidth) maxStack += 1
            if (lock != null) maxStack += 1
            visitMaxs(maxStack, queryIndex + 1)
        } else {
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
                val it = params[i]
                visitVarInsn(ALOAD, queryIndex)
                visitIntInsn(i)
                it.type.let { type ->
                    visitVarInsn(getLoad(type), it.stackNum)
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

            lock?.let {
                visitVarInsn(ALOAD, queryIndex)
                visitFieldInsn(GETSTATIC, lockModeOwner, lock, lockModeDescriptor)
                visitMethodInsn(INVOKEINTERFACE, queryFunOwner, "setLockMode", "($lockModeDescriptor)$queryFunDescriptor", true)
            }

            if (isList){
                visitVarInsn(ALOAD, queryIndex)
                visitMethodInsn(INVOKEINTERFACE, queryFunOwner, "getResultList", "()$listDescriptor", true)
            } else {
                visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, queryIndex)
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    implAccess.internalName,
                    "singleOrNull",
                    "($typedQueryDescriptor)$objectDescriptor",
                    false
                )
            }

            makeCast(this, returnTypeDescription)
            visitInsn(getReturn(returnTypeDescription))
            visitMaxs(6, queryIndex + 2)
        }

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