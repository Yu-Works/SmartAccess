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
import smartaccess.access.AccessMaker.pageResultDescriptor
import smartaccess.access.AccessMaker.stringDescriptor
import smartaccess.access.query.AbstractQuery
import smartaccess.item.Page
import smartaccess.jdbc.annotation.Execute
import smartaccess.jdbc.annotation.Select
import smartaccess.jpa.PagedQuery
import smartaccess.jpa.annotation.Lock
import java.lang.reflect.Method


object JpaAsyncAccessMaker : ServiceAccessMaker {

    override val baseMethods = JpaAsyncAccess::class.java.allMethod.map { it.name }

    private val queryOwner = Query::class.java.internalName
    private val queryDescriptor = Query::class.java.descriptor

    private val typedQueryOwner = TypedQuery::class.java.internalName
    private val typedQueryDescriptor = TypedQuery::class.java.descriptor

    private val pagedQueryOwner = PagedQuery::class.java.internalName
    private val pagedQueryDescriptor = PagedQuery::class.java.descriptor

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
        val typedFun = isModel || isList || isPage
        val funName = when {
            isPage -> "pagedQuery"
            isList -> "typedQuery"
            isModel -> "typedQuery"
            else -> "jpaQuery"
        }

        val funDescriptor = when (funName) {
            "pagedQuery" -> "($stringDescriptor$classDescriptor$continuationDescriptor)$objectDescriptor"
            "typedQuery" -> "($stringDescriptor$classDescriptor$continuationDescriptor)$objectDescriptor"
            else -> "($stringDescriptor$continuationDescriptor)$objectDescriptor"
        }
        val queryIsInterface = !isPage
        val queryOwner = when (funName) {
            "pagedQuery" -> pagedQueryOwner
            "typedQuery" -> typedQueryOwner
            else -> queryOwner
        }
        val queryDescriptor = when (funName) {
            "pagedQuery" -> pagedQueryDescriptor
            "typedQuery" -> typedQueryDescriptor
            else -> queryDescriptor
        }
        val resultFunName =
            if (isPage) "getPageResult"
            else if (isList) "getResultList"
            else "singleOrNull"
        val resultFunTypeDescriptor = when (resultFunName) {
            "getPageResult" -> pageResultDescriptor
            "getResultList" -> listDescriptor
            else -> objectDescriptor
        }
        val resultFunAtThis = resultFunName == "singleOrNull"

        val queryAble = QueryAble(
            typedFun,
            funName,
            funDescriptor,
            queryIsInterface,
            queryOwner,
            queryDescriptor,
            resultFunName,
            resultFunTypeDescriptor,
            resultFunAtThis
        )
        makeAsyncFunction(
            method,
            implAccess,
            access,
            moduleType,
            query,
            suspendContextClass,
            queryAble
        )
    }

    data class QueryAble(
        val typedFun: Boolean,
        val funName: String,
        val funDescriptor: String,
        val queryIsInterface: Boolean,
        val queryOwner: String,
        val queryDescriptor: String,
        val resultFunName: String,
        val resultFunTypeDescriptor: String,
        val resultFunAtThis: Boolean
    )

    fun MethodVisitor.makeAsyncFunction(
        method: Method,
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        query: String,
        suspendContextClass: String?,
        queryFun: QueryAble
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
        val paramNum = params.size
        val noContextParamNum = paramNum - 1
        var noPageParamNum = noContextParamNum
        if (noPageParamNum > 1 && method.parameterTypes[noPageParamNum - 1] == Page::class.java)
            noPageParamNum--

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

        // check context
        apply {
            // 判断是否是第一次调用
            apply {
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
            }

            // 创建新的上下文
            apply {
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
            }
        }

        val resultIndex = nextStack()
        val queryIndex = nextStack()

        val switchDefaultLabel = Label()
        val switch0Label = Label()
        val switch1Label = Label()

        val executeLabel = Label()

        // 开始准备 switch
        apply {
            visitLabel(bodyLabel)
            visitFrame(
                F_APPEND,
                2,
                arrayOf<Any>(suspendContextClass!!, TOP),
                0,
                null
            )

            visitVarInsn(ALOAD, contextIndex)
            visitFieldInsn(GETFIELD, suspendContextClass, "result", objectDescriptor)
            visitVarInsn(ASTORE, resultIndex)

            visitVarInsn(ALOAD, contextIndex)
            visitFieldInsn(GETFIELD, suspendContextClass, "label", "I")
            visitTableSwitchInsn(0, 1, switchDefaultLabel, switch0Label, switch1Label)

            // switch case 0:
            apply {
                visitLabel(switch0Label)
                // 声明完整帧栈
                val fullStack = ArrayList<Any>()
                fullStack.add(thisOwner)
                method.parameters.forEach { fullStack.add(it.type.internalName) }
                fullStack.add(suspendContextClass)
                fullStack.add(objectOwner)
                visitFrame(F_FULL, params.size + 3, fullStack.toTypedArray(), 0, null)

                visitVarInsn(ALOAD, 0)
                visitLdcInsn(query)
                if (queryFun.typedFun) visitLdcInsn(Type.getType(moduleType))
                visitVarInsn(ALOAD, contextIndex)
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    implAccess.internalName,
                    queryFun.funName,
                    queryFun.funDescriptor,
                    false
                )
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
                    if (i >= noContextParamNum) return@forEachIndexed
                    visitVarInsn(ALOAD, contextIndex)
                    visitVarInsn(getLoad(it.type), it.stackNum)
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
                    if (i >= noContextParamNum) return@forEachIndexed
                    visitVarInsn(ALOAD, contextIndex)
                    visitFieldInsn(GETFIELD, suspendContextClass, "param$i", it.type)
                    visitVarInsn(getStore(it.type), it.stackNum)
                }
            }

            // execute
            apply {
                visitLabel(executeLabel)
                val fullStack = ArrayList<Any>()
                fullStack.add(thisOwner)
                method.parameters.forEach { fullStack.add(it.type.internalName) }
                fullStack.add(suspendContextClass)
                fullStack.add(objectOwner)
                fullStack.add(TOP)
                visitFrame(F_FULL, params.size + 4, fullStack.toTypedArray(), 0, null)

                visitVarInsn(ALOAD, resultIndex)
                visitTypeInsn(CHECKCAST, queryFun.queryOwner)
                visitVarInsn(ASTORE, queryIndex)


                val havePage = noPageParamNum < noContextParamNum
                val invokeQueryOpcode = if (queryFun.queryIsInterface) INVOKEINTERFACE else INVOKEVIRTUAL

                visitVarInsn(ALOAD, queryIndex)

                for (i in 0..< noPageParamNum) {
                    val it = params[i]
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
                        invokeQueryOpcode,
                        queryFun.queryOwner,
                        "setParameter",
                        "(ILjava/lang/Object;)${queryFun.queryDescriptor}",
                        queryFun.queryIsInterface
                    )
                }

                if (havePage) {
                    val pageIndex = params.get(noPageParamNum).stackNum
                    visitVarInsn(ALOAD, pageIndex)
                    visitMethodInsn(INVOKEVIRTUAL, pageOwner, "getStart", "()I", false)
                    visitMethodInsn(
                        invokeQueryOpcode,
                        queryFun.queryOwner,
                        "setFirstResult",
                        "(I)${queryFun.queryDescriptor}",
                        queryFun.queryIsInterface
                    )
                    visitVarInsn(ALOAD, pageIndex)
                    visitMethodInsn(INVOKEVIRTUAL, pageOwner, "getNum", "()I", false)
                    visitMethodInsn(
                        invokeQueryOpcode,
                        queryFun.queryOwner,
                        "setMaxResults",
                        "(I)${queryFun.queryDescriptor}",
                        queryFun.queryIsInterface
                    )
                }

                lock?.let {
                    visitFieldInsn(GETSTATIC, lockModeOwner, lock, lockModeDescriptor)
                    visitMethodInsn(
                        invokeQueryOpcode,
                        queryFun.queryOwner,
                        "setLockMode",
                        "($lockModeDescriptor)${queryFun.queryDescriptor}",
                        queryFun.queryIsInterface
                    )
                }

                visitInsn(POP)

                if (queryFun.resultFunAtThis) visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, queryIndex)
                if (queryFun.resultFunAtThis)
                    visitMethodInsn(
                        INVOKEVIRTUAL,
                        implAccess.internalName,
                        queryFun.resultFunName,
                        "(${queryFun.queryDescriptor})${queryFun.resultFunTypeDescriptor}",
                        false
                    )
                else
                    visitMethodInsn(
                        invokeQueryOpcode,
                        queryFun.queryOwner,
                        queryFun.resultFunName,
                        "()${queryFun.resultFunTypeDescriptor}",
                        queryFun.queryIsInterface
                    )

                val returnTypeDescription = method.returnType.descriptor

                if (queryFun.resultFunTypeDescriptor.length == 1){
                    val type = queryFun.resultFunTypeDescriptor
                    getTyped(type).let { typed ->
                        visitMethodInsn(INVOKESTATIC, typed, "valueOf", "($type)L$typed;", false)
                    }
                }

                makeCast(this, returnTypeDescription)
                visitInsn(getReturn(returnTypeDescription))
            }

            // switch default:
            apply {
                visitLabel(switchDefaultLabel)
                val fullStack = ArrayList<Any>()
                fullStack.add(thisOwner)
                method.parameters.forEach { fullStack.add(it.type.internalName) }
                visitFrame(F_FULL, params.size + 1, fullStack.toTypedArray(), 0, null)
//                visitFrame(F_SAME, 0, null, 0, null)
                visitTypeInsn(NEW, "java/lang/IllegalStateException")
                visitInsn(DUP)
                visitLdcInsn("call to 'resume' before 'invoke' with coroutine")
                visitMethodInsn(
                    INVOKESPECIAL,
                    "java/lang/IllegalStateException",
                    "<init>",
                    "(Ljava/lang/String;)V",
                    false
                )
                visitInsn(ATHROW)
            }
        }

        val endLabel = Label()
        visitLabel(endLabel)
        visitLocalVariable("this", thisDescriptor, null, startLabel, switchDefaultLabel, 0)
        params.forEachIndexed { i, it ->
            visitLocalVariable("p$i", it.type, null, startLabel, switchDefaultLabel, it.stackNum)
        }
        visitLocalVariable("context", continuationDescriptor, null, startLabel, switchDefaultLabel, contextIndex)
        visitLocalVariable("result", objectDescriptor, null, bodyLabel, switchDefaultLabel, resultIndex)
        visitLocalVariable("query", queryFun.queryDescriptor, null, executeLabel, switchDefaultLabel, queryIndex)

        visitMaxs(if (hasWidth) 7 else 6, queryIndex + 2)
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
        makeAsyncFunction(
            method,
            implAccess,
            access,
            moduleType,
            query,
            suspendContextClass,
            QueryAble(
                false,
                "executeQuery",
                "($stringDescriptor$continuationDescriptor)$objectDescriptor",
                true,
                queryOwner,
                queryDescriptor,
                "executeUpdate",
                "I",
                false
            )
        )
    }

    override fun AbstractQuery.serialize(moduleType: Class<*>): String =
        toSqlString(moduleType.simpleName, needSelect = false, needIndex = true)

    override fun Method.haveSelect(): String? = annotation<Select>()?.value

    override fun Method.haveExecute(): String? = annotation<Execute>()?.value


    private fun MethodVisitor.visitVarInsn(param: MethodPara) {
        visitVarInsn(getLoad(param.type), param.stackNum)
    }
}

