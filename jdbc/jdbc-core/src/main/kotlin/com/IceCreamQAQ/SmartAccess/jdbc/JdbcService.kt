package com.IceCreamQAQ.SmartAccess.jdbc

import com.IceCreamQAQ.SmartAccess.DBContext
import com.IceCreamQAQ.SmartAccess.DBService
import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.access.AccessMetadataProvider
import com.IceCreamQAQ.SmartAccess.jdbc.pool.JDBCPool
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.hasAnnotation
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import jakarta.persistence.Entity
import jakarta.persistence.Table
import javax.inject.Named
import javax.sql.DataSource


@Named("JDBC")
class JdbcService(@Config db: ObjectNode) : DBService {

    var connectMap = HashMap<String, DataSource>()
    override val context: DBContext
        get() = TODO("Not yet implemented")

    override fun initDatabase(name: String, config: ObjectNode) {
        connectMap[name] = JDBCPool.supportPool.createDataSource(name, config)
    }

    override fun startDatabase(name: String, models: List<Class<*>>) {

    }

    override fun closeDatabase(name: String) {
        connectMap[name]?.connection?.close()
    }

    override fun isModel(clazz: Class<*>): Boolean = clazz.hasAnnotation<Entity>() || clazz.hasAnnotation<Table>()

    override fun createAccess(
        accessClass: Class<out Access<*, *>>,
        modelClass: Class<*>,
        metadataProvider: AccessMetadataProvider
    ): Access<*, *> {
        TODO("Not yet implemented")
    }

    override fun close() {
        connectMap.values.forEach { it.connection.close() }
    }
}