package smartaccess.jpa.access

import smartaccess.access.Access
import smartaccess.annotation.ProvideAccessTemple
import smartaccess.item.Page
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import rain.function.annotation
import smartaccess.item.PageResult
import smartaccess.jpa.annotation.SearchRewriter
import java.io.Serializable
import java.sql.Connection

@ProvideAccessTemple
interface JpaAccess<T, PK : Serializable> : Access<T, PK> {

    val modelName: String

    val searchRewriter: QueryRewriter?
        get() = modelType.annotation<SearchRewriter>()?.value?.java?.newInstance()
    val executeRewriter: QueryRewriter?
        get() = modelType.annotation<SearchRewriter>()?.value?.java?.newInstance()

    fun getConnection(): Connection
    fun getEntityManager(): EntityManager

    fun get(id: PK): T?
    fun delete(id: PK)

    fun save(entity: T)
    fun update(entity: T)

    fun saveOrUpdate(entity: T)

    fun where(paras: Map<String, Any>)
    fun where(paras: Map<String, Any>, page: Page)

    fun findAll(): List<T>
    fun findAll(page: Page): List<T>
    fun findPage(page: Page): PageResult<T>

    fun single(queryString: String, vararg params: Any?): T?
    fun single(queryString: String, lock: LockModeType, vararg params: Any?): T?

    fun count(queryString: String, vararg params: Any?): Long

    fun list(queryString: String, vararg params: Any?): List<T>
    fun list(queryString: String, lock: LockModeType, vararg params: Any?): List<T>
    fun list(queryString: String, page: Page, vararg params: Any?): List<T>
    fun list(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): List<T>

    fun page(queryString: String, page: Page, vararg params: Any?): PageResult<T>
    fun page(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): PageResult<T>

    fun execute(queryString: String, vararg para: Any): Int

    fun jpaQuery(qlString: String): Query
    fun <E> typedQuery(qlString: String, type: Class<E>): TypedQuery<E>

}