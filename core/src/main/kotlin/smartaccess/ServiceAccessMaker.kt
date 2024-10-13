package smartaccess

import smartaccess.access.query.AbstractQuery
import org.objectweb.asm.MethodVisitor
import java.lang.reflect.Method

interface ServiceAccessMaker {

    val baseMethods: List<String>

    fun MethodVisitor.makeConstructor(
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        constructorDescriptor: String
    )

    fun MethodVisitor.makeSelect(
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
    )

    fun MethodVisitor.makeExecute(
        method: Method,
        implAccess: Class<*>,
        access: Class<*>,
        moduleType: Class<*>,
        primaryKeyType: Class<*>,
        query: String
    )

    fun AbstractQuery.serialize(moduleType: Class<*>): String

    fun Method.haveSelect(): String?
    fun Method.haveExecute(): String?

}