package smartaccess.jpa.access

import jakarta.persistence.LockModeType
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import rain.function.*
import smartaccess.ServiceAccessMaker
import smartaccess.access.AccessMaker.classDescriptor
import smartaccess.access.AccessMaker.continuationDescriptor
import smartaccess.access.AccessMaker.descriptor
import smartaccess.access.AccessMaker.internalName
import smartaccess.access.AccessMaker.listDescriptor
import smartaccess.access.AccessMaker.objectDescriptor
import smartaccess.access.AccessMaker.objectOwner
import smartaccess.access.AccessMaker.pageOwner
import smartaccess.access.AccessMaker.stringDescriptor
import smartaccess.access.query.AbstractQuery
import smartaccess.jdbc.annotation.Execute
import smartaccess.jdbc.annotation.Select
import smartaccess.jpa.annotation.Lock
import java.lang.reflect.Method


object JpaAsyncAccessMaker : ServiceAccessMaker {

    override val baseMethods = JpaAsyncAccess::class.java.allMethod.map { it.name }

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
        realType: Class<*>,
        suspendContextClass: String?
    ) {
        val thisOwner = access.internalName + "\$Impl"
        val thisDescriptor = "L$thisOwner;"

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
        var methodStack = (params.lastOrNull()?.let { it.stackNum + it.stackSize } ?: 0)
        val hasWidth = params.any { it.stackSize == 2 }

        fun nextStack(width: Boolean = false): Int {
            val i = methodStack
            methodStack += if (width) 2 else 1
            return i
        }

        val lock = method.getAnnotation(Lock::class.java)?.value?.name

        val startLabel = Label()

        val newContextLabel = Label()
        val bodyLabel = Label()
        val contextIndex = nextStack()

        visitVarInsn(params.last())
        visitTypeInsn(INSTANCEOF, suspendContextClass)
        visitJumpInsn(IFEQ, newContextLabel)
        visitVarInsn(params.last())
        visitTypeInsn(CHECKCAST, suspendContextClass)
        visitVarInsn(ASTORE, contextIndex)
        visitVarInsn(ALOAD, contextIndex)
        visitFieldInsn(GETFIELD, suspendContextClass, "label", "I")
        visitLdcInsn(Int.MIN_VALUE)
        visitInsn(IAND)
        visitJumpInsn(IFEQ, newContextLabel)
        visitVarInsn(ALOAD, contextIndex)
        visitInsn(DUP)
        visitFieldInsn(GETFIELD, suspendContextClass, "label", "I")
        visitLdcInsn(Int.MIN_VALUE)
        visitInsn(ISUB)
        visitFieldInsn(PUTFIELD, suspendContextClass, "label", "I")
        visitJumpInsn(GOTO, bodyLabel)


        visitLabel(newContextLabel)
        visitFrame(F_SAME, 0, null, 0, null)

        visitTypeInsn(NEW, suspendContextClass)
        visitInsn(DUP)
        visitVarInsn(ALOAD, 0)
        visitVarInsn(params.last())
        visitMethodInsn(
            INVOKESPECIAL,
            suspendContextClass,
            "<init>",
            "(L${access.internalName};$continuationDescriptor)V",
            false
        )
        visitVarInsn(ASTORE, contextIndex)

        visitLabel(bodyLabel)
        visitFrame(
            F_APPEND,
            2,
            arrayOf<Any>(suspendContextClass!!, TOP),
            0,
            null
        )

        val resultIndex = nextStack()
        visitVarInsn(ALOAD, contextIndex)
        visitFieldInsn(GETFIELD, suspendContextClass, "result", objectDescriptor)
        visitVarInsn(ASTORE, resultIndex)


        val switchDefaultLabel = Label()
        val switch0Label = Label()
        val switch1Label = Label()

        val executeLabel = Label()

        val (queryFunOwner, queryFunDescriptor) =
            if (isModel) typedQueryOwner to typedQueryDescriptor
            else queryOwner to queryDescriptor


        visitVarInsn(ALOAD, contextIndex)
        visitFieldInsn(GETFIELD, suspendContextClass, "label", "I")
        visitTableSwitchInsn(0, 1, switchDefaultLabel, switch0Label, switch1Label)

        // switch case 0:
        apply {
            visitLabel(switch0Label)
            val fullStack = ArrayList<Any>()
            fullStack.add(thisOwner)
            method.parameters.forEach { fullStack.add(it.type.internalName) }
            fullStack.add(suspendContextClass)
            fullStack.add(objectOwner)
            visitFrame(F_FULL, params.size + 3, fullStack.toTypedArray(), 0, null)
//            visitFrame(F_SAME, 0, null, 0, null)

            visitVarInsn(ALOAD, 0)
            visitLdcInsn(query)

            if (isModel) {
                visitLdcInsn(Type.getType(moduleType))
                visitVarInsn(ALOAD, contextIndex)
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    implAccess.internalName,
                    "typedQuery",
                    "($stringDescriptor$classDescriptor$continuationDescriptor)$objectDescriptor",
                    false
                )
            } else {
                visitVarInsn(ALOAD, contextIndex)
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    implAccess.internalName,
                    "jpaQuery",
                    "($stringDescriptor$continuationDescriptor)$objectDescriptor",
                    false
                )
            }
            visitVarInsn(ASTORE, resultIndex)
            visitVarInsn(ALOAD, resultIndex)
            visitMethodInsn(
                INVOKESTATIC,
                "kotlin/coroutines/intrinsics/IntrinsicsKt",
                "getCOROUTINE_SUSPENDED",
                "()Ljava/lang/Object;",
                false
            )
            visitJumpInsn(IF_ACMPNE, executeLabel)
            visitVarInsn(ALOAD, contextIndex)
            visitInsn(ICONST_1)
            visitFieldInsn(PUTFIELD, suspendContextClass, "label", "I")
            params.forEachIndexed { i, it ->
                if (i >= (params.size - 1)) return@forEachIndexed
                visitVarInsn(ALOAD, contextIndex)
                visitVarInsn(ALOAD, it.stackNum)
                visitFieldInsn(PUTFIELD, suspendContextClass, "param$i", it.type)
            }
            visitVarInsn(ALOAD, resultIndex)
            visitInsn(ARETURN)
        }


        // switch case 1:
        apply {
            visitLabel(switch1Label)
            visitFrame(F_SAME, 0, null, 0, null)
            visitVarInsn(ALOAD, contextIndex)
            visitMethodInsn(INVOKESTATIC, "kotlin/ResultKt", "throwOnFailure", "(Ljava/lang/Object;)V", false);
            params.forEachIndexed { i, it ->
                if (i >= (params.size - 1)) return@forEachIndexed
                visitVarInsn(ALOAD, contextIndex)
                visitFieldInsn(GETFIELD, suspendContextClass, "param$i", it.type)
                visitVarInsn(ASTORE, it.stackNum)
            }
        }

        // execute
        val queryIndex = nextStack()
        apply {
            visitLabel(executeLabel)
            val fullStack = ArrayList<Any>()
            fullStack.add(thisOwner)
            method.parameters.forEach { fullStack.add(it.type.internalName) }
            fullStack.add(suspendContextClass)
            fullStack.add(objectOwner)
            fullStack.add(TOP)
            visitFrame(F_FULL, params.size + 4, fullStack.toTypedArray(), 0, null)
//        visitFrame(F_APPEND, 1, arrayOf(queryFunOwner), 0, null)
//        if (!isModel) error("暂不支持非 Model 的返回值类型。")

            visitVarInsn(ALOAD, resultIndex)
            visitTypeInsn(CHECKCAST, queryFunOwner)
            visitVarInsn(ASTORE, queryIndex)


//        val havePage = method.parameters[params.size - 2].type == Page::class.java
            val havePage = false
            val paramCount = (if (havePage) method.parameterCount - 1 else method.parameterCount) - 1

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
                visitMethodInsn(
                    INVOKEINTERFACE,
                    queryFunOwner,
                    "setLockMode",
                    "($lockModeDescriptor)$queryFunDescriptor",
                    true
                )
            }

            if (isList) {
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
            val returnTypeDescription = method.returnType.descriptor

            makeCast(this, returnTypeDescription)
            visitInsn(getReturn(returnTypeDescription))
        }

        // switch default:
        apply {
            visitLabel(switchDefaultLabel)
            visitFrame(F_SAME, 0, null, 0, null)
            visitTypeInsn(NEW, "java/lang/IllegalStateException")
            visitInsn(DUP)
            visitLdcInsn("call to 'resume' before 'invoke' with coroutine")
            visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false)
            visitInsn(ATHROW)
        }
        val endLabel = Label()
        visitLabel(endLabel)
        visitLocalVariable("this", thisDescriptor, null, startLabel, switchDefaultLabel, 0)
        params.forEachIndexed { i, it ->
            visitLocalVariable("p$i", it.type, null, startLabel, switchDefaultLabel, it.stackNum)
        }
        visitLocalVariable("context", continuationDescriptor, null, startLabel, switchDefaultLabel, contextIndex)
        visitLocalVariable("result", objectDescriptor, null, bodyLabel, switchDefaultLabel, resultIndex)
        visitLocalVariable("query", queryFunDescriptor, null, executeLabel, switchDefaultLabel, queryIndex)

        visitMaxs(if (hasWidth) 7 else 6, queryIndex + 2)
//        visitMaxs(0, 0)
        visitEnd()
    }

    override fun MethodVisitor.makeExecute(
        method: Method,
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        query: String,
        suspendContextClass: String?
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


    private fun MethodVisitor.visitVarInsn(param: MethodPara) {
        visitVarInsn(getLoad(param.type), param.stackNum)
    }
}

