package smartaccess.jpa.access

import jakarta.persistence.*
import smartaccess.annotation.Database
import smartaccess.item.Page
import smartaccess.jdbc.access.JDBCPageAble
import smartaccess.jpa.db.JpaContext
import rain.function.allField
import rain.function.annotation
import rain.function.hasAnnotation
import smartaccess.item.PageResult
import java.io.Serializable
import java.sql.Connection
import kotlin.jvm.internal.Intrinsics

abstract class JpaAsyncAccessBase<T, PK : Serializable>(
    var context: JpaContext,
    var pageable: JDBCPageAble?,
    @JvmField
    val _modelType: Class<T>,
    @JvmField
    val _primaryKeyType: Class<PK>
) : JpaAsyncAccess<T, PK> {

    @JvmField
    val _modelName = _modelType.simpleName

    override val modelType: Class<T>
        get() = _modelType

    override val primaryKeyType: Class<PK>
        get() = _primaryKeyType

    override val modelName
        get() = _modelName

    val _searchRewriter = searchRewriter ?: QueryRewriter { it }
    val _executeRewriter = executeRewriter ?: QueryRewriter { it }

    val database = _modelType.annotation<Database>()?.value ?: "default"
    val primaryKeyName = _modelType.allField.first { it.hasAnnotation<Id>() }.name


    val selectQueryString = "from $_modelName"
    val countQueryString = "select count($primaryKeyName) from $_modelName"
    val deleteQueryString = "delete form $_modelName where $primaryKeyName = ?0"

    override suspend fun getConnection(): Connection = getEntityManager().unwrap(Connection::class.java)
    override suspend fun getEntityManager(): EntityManager = context.getEntityManagerAsync(database)

    override suspend fun where(paras: Map<String, Any>) {
        TODO("Not yet implemented")
    }

    override suspend fun where(paras: Map<String, Any>, page: Page) {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): List<T> = list(selectQueryString)

    override suspend fun findAll(page: Page): List<T> = list(selectQueryString, page)

    override suspend fun findPage(page: Page) = page(selectQueryString, page)

    override suspend fun jpaQuery(qlString: String): Query {
        return getEntityManager().createQuery(_searchRewriter(qlString))
    }

    override suspend fun <E> typedQuery(qlString: String, type: Class<E>): TypedQuery<E> {
        return getEntityManager().createQuery(_searchRewriter(qlString), type)
    }

    fun <E> singleOrNull(query: TypedQuery<E>): E? =
        try {
            query.singleResult
        } catch (e: NoResultException) {
            null
        }

    override suspend fun single(queryString: String, vararg params: Any?): T? {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        return singleOrNull(typedQuery)
    }

    override suspend fun single(queryString: String, lock: LockModeType, vararg params: Any?): T? {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        typedQuery.lockMode = lock
        return singleOrNull(typedQuery)
    }

    override suspend fun count(queryString: String, vararg params: Any?): Long {
        val query = jpaQuery(queryString)
        params.forEachIndexed { i, it -> query.setParameter(i, it) }
        return query.singleResult as Long
    }

    override suspend fun list(queryString: String, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        return typedQuery.resultList
    }

    override suspend fun list(queryString: String, lock: LockModeType, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        typedQuery.lockMode = lock
        return typedQuery.resultList
    }

    override suspend fun list(queryString: String, page: Page, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        typedQuery.setFirstResult(page.start)
        typedQuery.setMaxResults(page.num)
        return typedQuery.resultList
    }

    override suspend fun list(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        typedQuery.lockMode = lock
        typedQuery.setFirstResult(page.start)
        typedQuery.setMaxResults(page.num)
        return typedQuery.resultList
    }

    override suspend fun page(queryString: String, page: Page, vararg params: Any?): PageResult<T> =
        PageResult(count(query2countQuery(queryString), *params), list(queryString, page, *params))

    override suspend fun page(queryString: String, lock: LockModeType, page: Page, vararg params: Any?): PageResult<T> =
        PageResult(count(query2countQuery(queryString), *params), list(queryString, lock, page, *params))

    open fun query2countQuery(queryString: String): String {
        val index = queryString.indexOf("from")
        return "select count(id) " + queryString.substring(index)
    }

    override suspend fun execute(queryString: String, vararg para: Any): Int {
        val query = getEntityManager().createQuery(_executeRewriter(queryString))
        para.forEachIndexed { i, it -> query.setParameter(i, it) }
        return query.executeUpdate()
    }

    override suspend fun saveOrUpdate(entity: T) {
        val em = getEntityManager()
        if (!em.contains(entity)) em.persist(entity)
        else em.merge(entity)
    }

    override suspend fun update(entity: T) {
        getEntityManager().merge(entity)
    }

    override suspend fun save(entity: T) {
        getEntityManager().persist(entity)
    }

    override suspend fun delete(id: PK) {
        execute(deleteQueryString, id)
    }

    override suspend fun get(id: PK): T? {
        return getEntityManager().find(modelType, id)
    }

    suspend fun findByField(field: String): Long {
        val query = jpaQuery("select t from $_modelName t where t.$field = ?1")
        query.setParameter(1, field)
        return query.singleResult as Long
    }
}