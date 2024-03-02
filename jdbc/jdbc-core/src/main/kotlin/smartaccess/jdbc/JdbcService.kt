package smartaccess.jdbc

import smartaccess.access.Access
import smartaccess.access.AccessMetadataProvider
import smartaccess.jdbc.pool.JDBCPool
import jakarta.persistence.Entity
import jakarta.persistence.Table
import rain.di.Config
import rain.function.dataNode.ObjectNode
import rain.function.hasAnnotation
import smartaccess.DBContext
import smartaccess.DBService
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