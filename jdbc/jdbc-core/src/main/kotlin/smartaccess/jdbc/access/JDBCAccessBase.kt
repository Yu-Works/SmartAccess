package smartaccess.jdbc.access

import smartaccess.annotation.Database
import smartaccess.item.Page
import smartaccess.jdbc.db.JdbcContext
import jakarta.persistence.Table
import rain.function.annotation
import java.io.Serializable
import java.sql.Connection

abstract class JDBCAccessBase<T, PK : Serializable>(
    private val context: JdbcContext,
    private val pageable: JDBCPageAble,
    private val modelType: Class<T>,
    private val primaryKeyType: Class<PK>
) : JDBCAccess<T, PK> {

    val table = modelType.declaredFields.firstNotNullOfOrNull { it.annotation<Table>() }?.name
        ?: error("实体类 ${modelType.name} 没有 Table 注解！")

    val database = modelType.annotation<Database>()?.value ?: "default"

    override fun getConnection(): Connection = context.getConnection(database)

    override fun findAll(): List<T> =
        list("select * from $table")

    override fun findAll(page: Page?): List<T> =
        list("select * from $table", page)


    override fun single(queryString: String, vararg params: Any?): T? =
        list(queryString, params, Page.single).firstOrNull()

    override fun list(queryString: String, vararg params: Any?): List<T> {
        TODO("Not yet implemented")
    }

    override fun list(queryString: String, page: Page, vararg params: Any?): List<T> {
        TODO("Not yet implemented")
    }

    override fun execute(query: String, vararg para: Any): Int {
        TODO("Not yet implemented")
    }

    override fun saveOrUpdate(entity: T) {
        TODO("Not yet implemented")
    }

    override fun update(entity: T) {
        TODO("Not yet implemented")
    }

    override fun save(entity: T) {
        TODO("Not yet implemented")
    }

    override fun delete(id: PK) {
        TODO("Not yet implemented")
    }

    override fun get(id: PK): T? {
        TODO("Not yet implemented")
    }

    fun query(query: String, vararg params: Any?): List<T> {
        val connection = getConnection()
        val statement = connection.prepareStatement(query)
        



        return list(query, params, Page.single)
    }
}