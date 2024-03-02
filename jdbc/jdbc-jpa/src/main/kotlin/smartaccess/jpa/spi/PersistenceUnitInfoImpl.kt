package smartaccess.jpa.spi

import jakarta.persistence.SharedCacheMode
import jakarta.persistence.ValidationMode
import jakarta.persistence.spi.ClassTransformer
import jakarta.persistence.spi.PersistenceUnitInfo
import jakarta.persistence.spi.PersistenceUnitTransactionType
import rain.function.dataNode.ObjectNode
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import javax.sql.DataSource

class PersistenceUnitInfoImpl(
    private val persistenceProviderClass: String,
    databaseConfig: ObjectNode,
    private var jta: Boolean,
    private var dataSource: DataSource,
    private val classLoader: ClassLoader,
    private val models: List<Class<*>>
) : PersistenceUnitInfo {

    private val properties: Properties = Properties()

    init {
        properties["url"] = databaseConfig["url"]!!.asString()
        properties["javax.persistence.transaction"] = if (jta) "JTA" else "RESOURCE_LOCAL"
        properties["jpa.dialect"] = databaseConfig["dialect"]!!.asString()
        properties["username"] = databaseConfig["username"]!!.asString()
        properties["javax.persistence.provider"] = persistenceProviderClass
        properties["password"] = databaseConfig["password"]!!.asString()
        properties["driver"] = databaseConfig["driver"]!!.asString()
    }

    override fun getPersistenceUnitName(): String {
        return "default"
    }

    override fun getPersistenceProviderClassName(): String {
        return persistenceProviderClass
    }

    override fun getTransactionType(): PersistenceUnitTransactionType {
        return if (jta) PersistenceUnitTransactionType.JTA else PersistenceUnitTransactionType.RESOURCE_LOCAL
    }

    override fun getJtaDataSource(): DataSource? {
        return if (jta) dataSource else null
    }

    override fun getNonJtaDataSource(): DataSource? {
        return if (jta) null else dataSource
    }

    override fun getMappingFileNames(): List<String> {
        return emptyList()
    }

    override fun getJarFileUrls(): List<URL> {
        return emptyList()
    }

    override fun getPersistenceUnitRootUrl(): URL {
        return try {
            File(".").toURI().toURL()
        } catch (e: IOException) {
            throw e
        }
    }

    override fun getManagedClassNames(): List<String> {
        return models.map { it.name }
    }

    override fun excludeUnlistedClasses(): Boolean {
        return true
    }

    override fun getSharedCacheMode(): SharedCacheMode {
        return SharedCacheMode.UNSPECIFIED
    }

    override fun getValidationMode(): ValidationMode {
        return ValidationMode.AUTO
    }

    override fun getProperties(): Properties {
        return properties
    }

    override fun getPersistenceXMLSchemaVersion(): String {
        return "2.2"
    }

    override fun getClassLoader(): ClassLoader {
        return classLoader
    }

    override fun addTransformer(transformer: ClassTransformer) {}
    override fun getNewTempClassLoader(): ClassLoader {
        return classLoader
    }

}