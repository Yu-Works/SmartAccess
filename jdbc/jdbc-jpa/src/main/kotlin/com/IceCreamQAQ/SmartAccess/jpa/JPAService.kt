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
import jakarta.persistence.Table
import javax.inject.Named
import javax.sql.DataSource

@Named("JPA")
abstract class JPAService(@Config db: ObjectNode) : DBService {

    override fun isModel(clazz: Class<*>): Boolean = clazz.hasAnnotation<Entity>() || clazz.hasAnnotation<Table>()

    override fun createAccess(
        accessClass: Class<out Access<*, *>>,
        modelClass: Class<*>,
        metadataProvider: AccessMetadataProvider
    ): Access<*, *> {
        TODO("Not yet implemented")
    }

}