package com.IceCreamQAQ.SmartAccess.jpa.access

import com.IceCreamQAQ.SmartAccess.annotation.Database
import com.IceCreamQAQ.SmartAccess.item.Page
import com.IceCreamQAQ.SmartAccess.jdbc.access.JDBCPageAble
import com.IceCreamQAQ.SmartAccess.jpa.db.JpaContext
import com.IceCreamQAQ.Yu.allField
import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.hasAnnotation
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import java.io.Serializable
import java.sql.Connection

open class JpaAccessBase<T, PK : Serializable>(
    var context: JpaContext,
    var pageable: JDBCPageAble?,
    var modelType: Class<T>,
    var primaryKeyType: Class<PK>
) : JpaAccess<T, PK> {

    val database = modelType.annotation<Database>()?.value ?: "default"
    val primaryKeyName = modelType.allField.first { it.hasAnnotation<Id>() }.name

    val modelName = modelType.simpleName
    val selectQueryString = "from $modelName"
    val countQueryString = "select count($primaryKeyName) from $modelName"
    val deleteQueryString = "delete form $modelName where $primaryKeyName = ?0"

    override fun getConnection(): Connection = getEntityManager().unwrap(Connection::class.java)
    override fun getEntityManager(): EntityManager = context.getEntityManager(database)

    override fun where(paras: Map<String, Any>) {
        TODO("Not yet implemented")
    }

    override fun where(paras: Map<String, Any>, page: Page) {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<T> = list(selectQueryString)

    override fun findAll(page: Page?): List<T> = list(selectQueryString, page)

    override fun jpaQuery(qlString: String): Query {
        return getEntityManager().createQuery(qlString)
    }

    override fun <E> typedQuery(qlString: String, type: Class<E>): TypedQuery<E> {
        return getEntityManager().createQuery(qlString, type)
    }

    override fun single(queryString: String, vararg params: Any?): T? {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        return typedQuery.singleResult
    }

    override fun list(queryString: String, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        return typedQuery.resultList
    }

    override fun list(queryString: String, page: Page, vararg params: Any?): List<T> {
        val typedQuery = typedQuery(queryString, modelType)
        params.forEachIndexed { i, it -> typedQuery.setParameter(i, it) }
        typedQuery.setFirstResult(page.start)
        typedQuery.setMaxResults(page.num)
        return typedQuery.resultList
    }

    override fun execute(queryString: String, vararg para: Any): Int {
        val query = jpaQuery(queryString)
        para.forEachIndexed { i, it -> query.setParameter(i, it) }
        return query.executeUpdate()
    }

    override fun saveOrUpdate(entity: T) {
        val em = getEntityManager()
        if (!em.contains(entity)) em.persist(entity)
        else em.merge(entity)
    }

    override fun update(entity: T) {
        getEntityManager().merge(entity)
    }

    override fun save(entity: T) {
        getEntityManager().persist(entity)
    }

    override fun delete(id: PK) {
        execute(deleteQueryString, id)
    }

    override fun get(id: PK): T? {
        return getEntityManager().find(modelType, id)
    }
}