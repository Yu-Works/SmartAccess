package smartaccess.jpa

import smartaccess.DBService
import smartaccess.access.Access
import smartaccess.access.AccessMaker
import smartaccess.access.AccessMetadataProvider
import smartaccess.jdbc.access.JDBCPageAble
import smartaccess.jdbc.pool.JDBCPool
import smartaccess.jpa.db.JpaContext
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import rain.classloader.AppClassloader
import rain.function.dataNode.ObjectNode
import rain.function.hasAnnotation
import smartaccess.jpa.access.*
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
        val (classByte, needs) = AccessMaker(
            JpaAsyncAccessBase::class.java,
            accessClass,
            modelClass,
            primaryType,
            JpaAsyncAccessMaker
        )
        File("tmp/classOutput/" + accessClass.name + "\$Impl.class").writeBytes(classByte)
        val classAccess = appClassloader.define(accessClass.name + "\$Impl", classByte)
        needs.forEach { (name, bytes) ->
            File("tmp/classOutput/$name.class").writeBytes(bytes)
            appClassloader.define(name, bytes)
        }
        return classAccess.getConstructor(
            JpaContext::class.java,
            JDBCPageAble::class.java,
            Class::class.java,
            Class::class.java
        ).newInstance(context, null, modelClass, primaryType) as Access<*, *>
    }

}