package smartaccess.jpa

import jakarta.persistence.*
import smartaccess.item.PageResult

class PagedQuery<T : Any>(
    private val searchQuery: TypedQuery<T>,
    private val countQuery: Query
) {

    fun setMaxResults(max: Int): PagedQuery<T> {
        searchQuery.maxResults = max
        return this
    }

    fun setFirstResult(first: Int): PagedQuery<T>{
        searchQuery.firstResult = first
        return this
    }

    fun setParameter(parameter: String, data: Any?): PagedQuery<T>{
        searchQuery.setParameter(parameter, data)
        countQuery.setParameter(parameter, data)
        return this
    }

    fun setParameter(parameter: Int, data: Any?): PagedQuery<T>{
        searchQuery.setParameter(parameter, data)
        countQuery.setParameter(parameter, data)
        return this
    }

    fun setLockMode(var1: LockModeType?): PagedQuery<T>{
        searchQuery.lockMode = var1
        return this
    }

    fun getPageResult(): PageResult<T> {
        val total = countQuery.singleResult as Long
        val data = searchQuery.resultList
        return PageResult(total, data)
    }
}