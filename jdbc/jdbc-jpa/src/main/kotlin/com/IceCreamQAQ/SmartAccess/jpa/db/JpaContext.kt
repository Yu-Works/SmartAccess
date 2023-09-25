package com.IceCreamQAQ.SmartAccess.jpa.db

import com.IceCreamQAQ.SmartAccess.DBContext
import com.IceCreamQAQ.SmartAccess.db.transaction.DBTransaction
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory

class JpaContext(private val emfMap: HashMap<String, EntityManagerFactory>) : DBContext {

    private val jpaConnectionMap = HashMap<String, ThreadLocal<EntityManager>>()

    fun getEntityManager(name: String): EntityManager {
        val emtl = jpaConnectionMap.getOrPut(name) {
            emfMap[name]?.let { ThreadLocal() } ?: error("未存在名为 $name 的数据库上下文！")
        }
        var em = (emtl).get()
        if (em == null || !em.isOpen) {
            em = emfMap[name]!!.createEntityManager()
            emtl.set(em)
        }
        return em
    }

    fun closeEntityManager(name: String) {
        val emtl = jpaConnectionMap.getOrPut(name) {
            emfMap[name]?.let { ThreadLocal() } ?: error("未存在名为 $name 的数据库上下文！")
        }
        val em = emtl.get()
        if (em.isOpen) {
            em.flush()
            em.close()
        }
        emtl.remove()
    }

    override fun beginTransactionSync(database: String): DBTransaction {
        return JpaTransaction(getEntityManager(database).transaction.apply { begin() })
    }

    override fun beginTransactionAsync(database: String): DBTransaction {
        TODO("Not yet implemented")
    }

}