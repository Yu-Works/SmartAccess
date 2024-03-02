package smartaccess.jdbc.access

import smartaccess.access.Access
import smartaccess.annotation.ProvideAccessTemple
import smartaccess.item.Page
import java.io.Serializable
import java.sql.Connection

@ProvideAccessTemple
interface JDBCAccess<T, PK : Serializable> : Access<T, PK> {

    fun getConnection(): Connection

    fun get(id: PK): T?
    fun delete(id: PK)

    fun save(entity: T)
    fun update(entity: T)

    fun saveOrUpdate(entity: T)

    fun where(paras: Map<String, Any>)
    fun where(paras: Map<String, Any>, page: Page)

    fun findAll(): List<T>
    fun findAll(page: Page?): List<T>

    fun single(queryString: String, vararg params: Any?): T?

    fun list(queryString: String, vararg params: Any?): List<T>
    fun list(queryString: String, page: Page, vararg params: Any?): List<T>

    fun selectOne(queryString: String, vararg params: Any?): Array<Any>
    fun selectItemOne(queryString: String, vararg params: Any?): Any?
    fun selectList(queryString: String, vararg params: Any?): List<Array<Any>>
    fun selectItemList(queryString: String, vararg params: Any?): List<Any>
    fun execute(query: String, vararg para: Any): Int

}