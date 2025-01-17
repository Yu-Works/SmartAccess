package smartaccess.jpa.access

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import rain.function.annotation
import smartaccess.access.Access
import smartaccess.annotation.ProvideAccessTemple
import smartaccess.item.Page
import smartaccess.item.PageResult
import smartaccess.jpa.annotation.SearchRewriter
import java.io.Serializable
import java.sql.Connection

@ProvideAccessTemple
interface JpaAsyncAccess<T, PK : Serializable> : Access<T, PK> {

    val modelName: String

    val searchRewriter: QueryRewriter?
        get() = modelType.annotation<SearchRewriter>()?.value?.java?.newInstance()
    val executeRewriter: QueryRewriter?
        get() = modelType.annotation<SearchRewriter>()?.value?.java?.newInstance()

    suspend fun getConnection(): Connection
    suspend fun getEntityManager(): EntityManager

    suspend fun get(id: PK): T?
    suspend fun delete(id: PK)

    suspend fun save(entity: T)
    suspend fun update(entity: T)

    suspend fun saveOrUpdate(entity: T)

    suspend fun where(paras: Map<String, Any>)
    suspend fun where(paras: Map<String, Any>, page: Page)

    suspend fun findAll(): List<T>
    suspend fun findAll(page: Page): List<T>
    suspend fun findPage(page: Page): PageResult<T>

    suspend fun single(queryString: String, vararg params: Any?): T?
    suspend fun single(queryString: String, lock: LockModeType, vararg params: Any?): T?

    suspend fun count(queryString: String, vararg params: Any?): Long

    suspend fun list(queryString: String, vararg params: Any?): List<T>
    suspend fun list(queryString: String, lock: LockModeType, vararg params: Any?): List<T>
    suspend fun list(queryString: String, page: Page, vararg params: Any?): List<T>
    suspend fun list(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): List<T>

    suspend fun page(queryString: String, page: Page, vararg params: Any?): PageResult<T>
    suspend fun page(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): PageResult<T>

    suspend fun execute(queryString: String, vararg para: Any): Int

    suspend fun jpaQuery(qlString: String): Query
    suspend fun <E> typedQuery(qlString: String, type: Class<E>): TypedQuery<E>
}