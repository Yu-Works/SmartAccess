package com.IceCreamQAQ.SmartAccess.jpa.access

import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.annotation.ProvideAccessTemple
import com.IceCreamQAQ.SmartAccess.item.Page
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import java.io.Serializable
import java.sql.Connection

@ProvideAccessTemple
interface JpaAccess<T, PK : Serializable> : Access<T, PK> {

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
    fun findAll(page: Page?): List<T>

    fun single(queryString: String, vararg params: Any?): T?

    fun list(queryString: String, vararg params: Any?): List<T>
    fun list(queryString: String, page: Page, vararg params: Any?): List<T>

    fun execute(queryString: String, vararg para: Any): Int

    fun jpaQuery(qlString: String): Query
    fun <E> typedQuery(qlString: String, type: Class<E>): TypedQuery<E>

}