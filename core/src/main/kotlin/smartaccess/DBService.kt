package smartaccess

import smartaccess.access.Access
import smartaccess.access.AccessMetadataProvider
import rain.api.annotation.AutoBind
import rain.function.dataNode.ObjectNode
import java.io.Closeable

@AutoBind
interface DBService : Closeable {

    val context: DBContext

    fun initDatabase(name: String, config: ObjectNode)
    fun startDatabase(name: String, models: List<Class<*>>)
    fun closeDatabase(name: String)

    fun isModel(clazz: Class<*>): Boolean

    fun createAccess(
        accessClass: Class<out Access<*, *>>,
        modelClass: Class<*>,
        metadataProvider: AccessMetadataProvider
    ): Access<*, *>

}