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
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

object AccessMaker {

    val Class<*>.internalName get() = Type.getInternalName(this)!!
    val Class<*>.descriptor get() = Type.getDescriptor(this)!!

    val pageOwner = Page::class.java.internalName
    val pageDescriptor = Page::class.java.descriptor

    val stringDescriptor = String::class.java.descriptor
    val classDescriptor = Class::class.java.descriptor
    val objectDescriptor = Any::class.java.descriptor
    val listDescriptor = List::class.java.descriptor

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


            access.allMethod.asSequence()
                .filter { !it.isDefault }
                .filter { it.name !in serviceAccessMaker.baseMethods }
                .forEach { method ->
                    fun makeMethod(): MethodVisitor {
                        return cw.visitMethod(ACC_PUBLIC, method.name, Type.getMethodDescriptor(method), null, null)
                    }

                    defaultImpl?.let {

                        runCatching {
                            it.getMethod(
                                method.name,
                                *arrayListOf(access).apply { addAll(method.parameterTypes) }.toTypedArray()
                            )
                        }.getOrNull()?.let { defaultMethod ->

                            var i = 1
                            fun getIndex(desc:String):Int{
                                val c = i
                                i += getTypedWidth(desc)
                                return c
                            }
                            makeMethod().apply {
                                visitCode()
                                visitVarInsn(ALOAD, 0)
                                method.parameterTypes.forEach{
                                    it.descriptor.let { desc->
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

                            return@forEach
                        }
                    }

                    val returnRelType = RelType.create(method.genericReturnType)
                    if (returnRelType.realClass.isArray) error("方法 ${method.fullName} 返回值不能为数组！请使用 List 接受返回值！")
                    val isList = List::class.java.isAssignableFrom(returnRelType.realClass)
                    if (isList && returnRelType.realClass != List::class.java) error("方法 ${method.fullName} 只能接受 List 类型！不能接受 List 其子类型！")
                    if (isList && returnRelType.generics == null) error("方法 ${method.fullName} 返回值为 List 时必须指定泛型类型！")

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

}