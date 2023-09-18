package com.IceCreamQAQ.SmartAccess.jpa.db

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory

class JpaContext(private val emfMap: HashMap<String, EntityManagerFactory>) {

    private val jpaConnectionMap = HashMap<String, ThreadLocal<EntityManager>>(emfMap.size)

    init {
        emfMap.keys.forEach { jpaConnectionMap[it] = ThreadLocal() }
    }

    fun getEntityManager(name: String): EntityManager {
        val emtl = jpaConnectionMap[name] ?: error("未存在名为 $name 的数据库上下文！")
        var em = (emtl).get()
        if (em == null || !em.isOpen) {
            em = emfMap[name]!!.createEntityManager()
            emtl.set(em)
        }
        return em
    }

    fun closeEntityManager(name: String) {
        val emtl = jpaConnectionMap[name] ?: error("未存在名为 $name 的数据库上下文！")
        val em = emtl.get()
        if (em.isOpen) {
            em.flush()
            em.close()
        }
        emtl.remove()
    }

}