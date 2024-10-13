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
import smartaccess.item.PageResult
import java.lang.reflect.Method
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

    operator fun invoke(
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        serviceAccessMaker: ServiceAccessMaker
    ): ByteArray {
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
                    fun makeMethod(): MethodVisitor {
                        return cw.visitMethod(ACC_PUBLIC, method.name, Type.getMethodDescriptor(method), null, null)
                    }

                    val returnRelType = RelType.create(method.genericReturnType)
                    if (returnRelType.realClass.isArray) error("方法 ${method.fullName} 返回值不能为数组！请使用 List 接受返回值！")
                    val isList = List::class.java.isAssignableFrom(returnRelType.realClass)
                    if (isList && returnRelType.realClass != List::class.java) error("方法 ${method.fullName} 只能接受 List 类型！不能接受 List 其子类型！")
                    if (isList && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 List 时必须指定泛型类型！")
                    val isPage = PageResult::class.java.isAssignableFrom(returnRelType.realClass)
                    if (isPage && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 PageResult 时必须指定泛型类型！")

                    val realType = if (isList) returnRelType.generics!![0].realClass else returnRelType.realClass
                    val isModel = realType == moduleType

                    method.haveExecute()?.let {
                        makeMethod().makeExecute(method, implAccess, access, moduleType, primaryKeyType, it)
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
                            realType
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
                                        realType
                                    ) else makeExecute(method, implAccess, access, moduleType, primaryKeyType, it)
                                }
                            }
                        }
                    }
                }
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