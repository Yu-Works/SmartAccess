package com.IceCreamQAQ.SmartAccess.jpa

import com.IceCreamQAQ.SmartAccess.DBService
import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.access.AccessMetadataProvider
import com.IceCreamQAQ.SmartAccess.db.DataSourceUtil
import com.IceCreamQAQ.SmartAccess.jdbc.pool.JDBCPool
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.hasAnnotation
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import java.io.Closeable
import javax.inject.Named
import javax.sql.DataSource

@Named("JPA")
abstract class JPAService(@Config db: ObjectNode) : DBService {

    override fun isModel(clazz: Class<*>): Boolean = clazz.hasAnnotation<Entity>() || clazz.hasAnnotation<Table>()

    val databaseMap = HashMap<String, ObjectNode>()
    val dataSourceMap = HashMap<String, DataSource>()
    val closeFunMap = HashMap<String, EntityManagerFactory>()

    override fun initDatabase(name: String, config: ObjectNode) {
        databaseMap[name] = config
        dataSourceMap[name] = JDBCPool.supportPool.createDataSource(name, config)
    }

    override fun closeDatabase(name: String) {
        closeFunMap[name]?.close()
    }

    override fun close() {
        closeFunMap.values.forEach { it.close() }
    }

    override fun createAccess(
        accessClass: Class<out Access<*, *>>,
        modelClass: Class<*>,
        metadataProvider: AccessMetadataProvider
    ): Access<*, *> {
        TODO("Not yet implemented")
    }

}