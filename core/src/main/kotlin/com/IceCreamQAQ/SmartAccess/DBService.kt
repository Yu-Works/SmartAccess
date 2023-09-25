package com.IceCreamQAQ.SmartAccess

import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.access.AccessMetadataProvider
import com.IceCreamQAQ.SmartAccess.db.transaction.DBTransaction
import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
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