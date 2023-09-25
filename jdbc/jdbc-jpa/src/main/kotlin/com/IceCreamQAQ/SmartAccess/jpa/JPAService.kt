package com.IceCreamQAQ.SmartAccess.jpa

import com.IceCreamQAQ.SmartAccess.DBContext
import com.IceCreamQAQ.SmartAccess.DBService
import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.access.AccessMetadataProvider
import com.IceCreamQAQ.SmartAccess.jdbc.access.JDBCPageAble
import com.IceCreamQAQ.SmartAccess.jdbc.pool.JDBCPool
import com.IceCreamQAQ.SmartAccess.jpa.access.JpaAccessBase
import com.IceCreamQAQ.SmartAccess.jpa.access.JpaAccessMaker
import com.IceCreamQAQ.SmartAccess.jpa.db.JpaContext
import com.IceCreamQAQ.Yu.hasAnnotation
import com.IceCreamQAQ.Yu.loader.AppClassloader
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import java.io.File
import javax.inject.Named
import javax.sql.DataSource

@Named("JPA")
abstract class JPAService(
    val appClassloader: AppClassloader
) : DBService {

    override fun isModel(clazz: Class<*>): Boolean =
        clazz.hasAnnotation<Entity>() || clazz.hasAnnotation<Table>()

    val databaseMap = HashMap<String, ObjectNode>()
    val dataSourceMap = HashMap<String, DataSource>()
    val entityManagerFactoryMap = HashMap<String, EntityManagerFactory>()

    override val context = JpaContext(entityManagerFactoryMap)

    override fun initDatabase(name: String, config: ObjectNode) {
        databaseMap[name] = config
        dataSourceMap[name] = JDBCPool.supportPool.createDataSource(name, config)
    }

    override fun closeDatabase(name: String) {
        entityManagerFactoryMap[name]?.close()
    }

    override fun close() {
        entityManagerFactoryMap.values.forEach { it.close() }
    }

    override fun createAccess(
        accessClass: Class<out Access<*, *>>,
        modelClass: Class<*>,
        metadataProvider: AccessMetadataProvider
    ): Access<*, *> {
        val primaryType = metadataProvider.getAccessPrimaryKeyType(accessClass)
        val classByte = JpaAccessMaker(
            JpaAccessBase::class.java,
            accessClass,
            modelClass,
            primaryType
        )
        val classAccess = appClassloader.define(accessClass.name + "\$Impl", classByte)
        File("tmp/classOutput/" + accessClass.name + "\$Impl.class").writeBytes(classByte)
        return classAccess.getConstructor(
            JpaContext::class.java,
            JDBCPageAble::class.java,
            Class::class.java,
            Class::class.java
        ).newInstance(context, null, modelClass, primaryType) as Access<*, *>
    }

}