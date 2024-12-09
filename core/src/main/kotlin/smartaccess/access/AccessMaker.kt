package smartaccess.access

import smartaccess.ServiceAccessMaker
import smartaccess.access.interpreter.MethodInterpreter
import smartaccess.item.Page
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import rain.function.*
import rain.function.type.RelType
import smartaccess.access.AccessMaker.descriptor
import smartaccess.access.AccessMaker.internalName
import smartaccess.item.PageResult
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

object AccessMaker {

    val Class<*>.internalName get() = Type.getInternalName(this)!!
    val Class<*>.descriptor get() = Type.getDescriptor(this)!!

    val pageOwner = Page::class.java.internalName
    val pageDescriptor = Page::class.java.descriptor

    val pageResultOwner = PageResult::class.java.internalName
    val pageResultDescriptor = PageResult::class.java.descriptor

    val stringDescriptor = String::class.java.descriptor
    val classDescriptor = Class::class.java.descriptor
    val listDescriptor = List::class.java.descriptor

    val objectOwner = Any::class.java.internalName
    val objectDescriptor = Any::class.java.descriptor

    val continuationOwner = Continuation::class.java.internalName
    val continuationDescriptor = Continuation::class.java.descriptor

    class AccessEntry(
        val access: ByteArray,
        val accessNeedClasses: Array<ByteArray>,
    )

    operator fun invoke(
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        serviceAccessMaker: ServiceAccessMaker
    ): Pair<ByteArray, Array<Pair<String,ByteArray>>> {
        val needClasses = ArrayList<Pair<String,ByteArray>>()

        val accessOwner = access.internalName
        val accessDescriptor = access.descriptor
        val implOwner = implAccess.internalName

        val modelDescriptor = moduleType.descriptor
        val pkDescriptor = primaryKeyType.descriptor

        val cw = ClassWriter(0)
        cw.visit(
            V1_8,
            ACC_PUBLIC,
            "${accessOwner}\$Impl",
            null,
            implOwner,
            arrayOf(accessOwner)
        )

        val isKotlin = access.hasAnnotation<Metadata>()
        val kAccess = access.kotlin

        val defaultImpl = access.declaredClasses.firstOrNull { it.name == "DefaultImpls" }
            ?: runCatching { Class.forName("${access.name}\$DefaultImpls") }.getOrNull()

        // 构造函数
        val constructorDescriptor =
            Type.getConstructorDescriptor(implAccess.kotlin.primaryConstructor!!.javaConstructor!!)

        serviceAccessMaker.run {
            cw.visitMethod(ACC_PUBLIC, "<init>", constructorDescriptor, null, null)
                .makeConstructor(implAccess, access, moduleType, primaryKeyType, constructorDescriptor)

            val defaultMethods = defaultImpl?.allMethod
                ?.mapNotNull { defaultMethod ->
                    runCatching {
                        access.getMethod(
                            defaultMethod.name,
                            *defaultMethod.parameterTypes.toMutableList().apply { removeAt(0) }.toTypedArray()
                        )
                    }.getOrNull()?.also { method ->
                        fun makeMethod(): MethodVisitor {
                            return cw.visitMethod(ACC_PUBLIC, method.name, Type.getMethodDescriptor(method), null, null)
                        }

                        var i = 1
                        fun getIndex(desc: String): Int {
                            val c = i
                            i += getTypedWidth(desc)
                            return c
                        }
                        makeMethod().apply {
                            visitCode()
                            visitVarInsn(ALOAD, 0)
                            method.parameterTypes.forEach {
                                it.descriptor.let { desc ->
                                    visitVarInsn(getLoad(desc), getIndex(desc))
                                }
                            }
                            visitMethodInsn(
                                INVOKESTATIC,
                                defaultImpl.internalName,
                                defaultMethod.name,
                                Type.getMethodDescriptor(defaultMethod),
                                false
                            )
                            if (method.returnType != Void.TYPE) visitInsn(getReturn(method.returnType.descriptor))
                            else visitInsn(RETURN)
                            visitMaxs(method.parameterTypes.size + 1, method.parameterTypes.size + 1)
                            visitEnd()
                        }
                    }
                } ?: emptyList()

            access.allMethod.asSequence()
                .filter { !it.isDefault }
                .filter { it.name !in serviceAccessMaker.baseMethods }
                .filter { it !in defaultMethods }
                .forEach { method ->
                    var suspendContextClass: String? = null
                    if (method.parameterCount > 0 && Continuation::class.java.isAssignableFrom(method.parameterTypes.last())) {
                        suspendContextClass = "${accessOwner}\$Impl\$${method.name}\$${needClasses.size}"
                        needClasses.add("${access.name}\$Impl\$${method.name}\$${needClasses.size}" to createSuspendMethodContextClass(access, method, needClasses.size))
                    }
                    fun makeMethod(): MethodVisitor =
                        cw.visitMethod(ACC_PUBLIC, method.name, Type.getMethodDescriptor(method), null, null)


                    val returnRelType = RelType.create(suspendContextClass?.let {
                        method.genericParameterTypes.last()
                            .let { it as ParameterizedType }
                            .let { it.actualTypeArguments[0] as WildcardType}
                            .let { it.lowerBounds[0] }
                    } ?: method.genericReturnType)
                    if (returnRelType.realClass.isArray) error("方法 ${method.fullName} 返回值不能为数组！请使用 List 接受返回值！")
                    val isList = List::class.java.isAssignableFrom(returnRelType.realClass)
                    if (isList && returnRelType.realClass != List::class.java) error("方法 ${method.fullName} 只能接受 List 类型！不能接受 List 其子类型！")
                    if (isList && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 List 时必须指定泛型类型！")
                    val isPage = PageResult::class.java.isAssignableFrom(returnRelType.realClass)
                    if (isPage && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 PageResult 时必须指定泛型类型！")

                    val realType = if (isList) returnRelType.generics!![0].realClass else returnRelType.realClass
                    val isModel = realType == moduleType

                    method.haveExecute()?.let {
                        makeMethod().makeExecute(
                            method,
                            implAccess,
                            access,
                            moduleType,
                            primaryKeyType,
                            it,
                            suspendContextClass
                        )
                    } ?: method.haveSelect()?.let {
                        makeMethod().makeSelect(
                            method,
                            implAccess,
                            access,
                            moduleType,
                            primaryKeyType,
                            it,
                            isList,
                            isPage,
                            isModel,
                            realType,
                            suspendContextClass
                        )
                    } ?: run {
                        MethodInterpreter(method.name).let { absQuery ->
                            absQuery.serialize(moduleType).let {
                                makeMethod().apply {
                                    if (absQuery.queryType < 5) makeSelect(
                                        method,
                                        implAccess,
                                        access,
                                        moduleType,
                                        primaryKeyType,
                                        it,
                                        isList,
                                        isPage,
                                        isModel,
                                        realType,
                                        suspendContextClass
                                    ) else makeExecute(
                                        method,
                                        implAccess,
                                        access,
                                        moduleType,
                                        primaryKeyType,
                                        it,
                                        suspendContextClass
                                    )
                                }
                            }
                        }
                    }
                }
        }

        cw.visitEnd()
        return cw.toByteArray() to needClasses.toTypedArray()
    }

    fun createSuspendMethodContextClass(instanceType: Class<*>, suspendMethod: Method, id: Int): ByteArray {
        val instanceOwner = instanceType.internalName
        val instanceDescriptor = instanceType.descriptor

        val paramNum = suspendMethod.parameterCount - 1
        val continuationImplOwner = "kotlin/coroutines/jvm/internal/ContinuationImpl"

        val thisOwner = "${instanceOwner}\$Impl\$${suspendMethod.name}\$$id"
        val thisDescriptor = "L$thisOwner;"
        val cw = ClassWriter(0)
        cw.visit(
            V1_8,
            ACC_PUBLIC,
            "${instanceOwner}\$Impl\$${suspendMethod.name}\$$id",
            null,
            continuationImplOwner,
            null
        )

        cw.visitField(ACC_PUBLIC, "instance", instanceType.descriptor, null, null).visitEnd()
        cw.visitField(ACC_PUBLIC, "label", "I", null, null).visitEnd()
        cw.visitField(ACC_PUBLIC, "result", objectDescriptor, null, null).visitEnd()

        suspendMethod.parameterTypes.forEachIndexed { index, type ->
            if (index >= paramNum) return@forEachIndexed
            cw.visitField(ACC_PUBLIC, "param$index", type.descriptor, null, null).visitEnd()
        }

        cw.visitMethod(ACC_PUBLIC, "<init>", "($instanceDescriptor$continuationDescriptor)V", null, null)
            .apply {
                visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, 1)
                visitFieldInsn(PUTFIELD, thisOwner, "instance", instanceDescriptor)
                visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, 2)
                visitMethodInsn(
                    INVOKESPECIAL,
                    continuationImplOwner,
                    "<init>",
                    "(Lkotlin/coroutines/Continuation;)V",
                    false
                )
                visitInsn(RETURN)
                visitMaxs(3, 3)
                visitEnd()
            }

        cw.visitMethod(ACC_PUBLIC, "invokeSuspend", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null)
            .apply {
                visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, 1)
                visitFieldInsn(PUTFIELD, thisOwner, "result", objectDescriptor)
                visitVarInsn(ALOAD, 0)
                visitInsn(DUP)
                visitFieldInsn(GETFIELD, thisOwner, "label", "I")
                visitLdcInsn(Int.MIN_VALUE)
                visitInsn(IOR)
                visitFieldInsn(PUTFIELD, thisOwner, "label", "I")
                visitVarInsn(ALOAD, 0)
                visitFieldInsn(GETFIELD, thisOwner, "instance", instanceDescriptor)
                repeat(paramNum) {
                    visitInsn(ACONST_NULL)
                }
                visitVarInsn(ALOAD, 0)
                visitTypeInsn(CHECKCAST, continuationOwner)
                visitMethodInsn(
                    INVOKEVIRTUAL,
                    instanceOwner,
                    suspendMethod.name,
                    Type.getMethodDescriptor(suspendMethod),
                    false
                )
                visitInsn(ARETURN)
                visitMaxs(3 + paramNum, 2)
                visitEnd()
            }

        cw.visitEnd()
        return cw.toByteArray()
    }

    fun findAllMethod(clazz: Class<*>, methods: MutableList<Method> = ArrayList()): List<Method> {
        clazz.declaredMethods.forEach {
            if (!methods.any { m -> it.name == m.name && it.parameterTypes.contentEquals(m.parameterTypes) })
                methods.add(it)
        }
        clazz.interfaces.forEach { findAllMethod(it, methods) }
        clazz.superclass?.let { findAllMethod(it, methods) }
        return methods
    }

    fun getTypedWidth(type: String): Int =
        when (type[0]) {
            'B', 'S', 'I', 'Z', 'F', 'C' -> 1
            'J', 'D' -> 2
            else -> 1
        }

}