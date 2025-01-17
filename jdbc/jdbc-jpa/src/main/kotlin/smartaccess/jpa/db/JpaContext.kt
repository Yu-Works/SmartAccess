package smartaccess.jpa.db

import smartaccess.DBContext
import smartaccess.db.transaction.DBTransaction
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class JpaContext(val emfMap: HashMap<String, EntityManagerFactory>) : DBContext {

    private val jpaConnectionMap = HashMap<String, ThreadLocal<EntityManager>>()

    class EntityManagerCoroutineContext(
        private val emfMap: HashMap<String, EntityManagerFactory>,
        private val databases: HashMap<String, EntityManager> = HashMap()
    ) : CoroutineContext.Element, Closeable {

        fun getDataBase(name: String): EntityManager {
            return databases.getOrPut(name) {
                emfMap[name]?.createEntityManager() ?: error("未存在名为 $name 的数据库上下文！")
            }
        }

        override fun close() {
            databases.values.forEach {
                if (it.isOpen) it.close()
            }
        }

        override val key: CoroutineContext.Key<EntityManagerCoroutineContext>
            get() = Key

        companion object Key : CoroutineContext.Key<EntityManagerCoroutineContext>
    }

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

    suspend inline fun <R> runWithDatabaseContext(crossinline block: suspend () -> R): R = coroutineScope {
        EntityManagerCoroutineContext(emfMap).use { async(it) { block() }.await() }
    }

    suspend inline fun <R> transaction(vararg databases: String, crossinline block: suspend () -> R): R {
        val dbs = databases.ifEmpty { arrayOf("default") }
        return runWithDatabaseContext {
            val transaction = dbs.map { beginTransactionAsync(it) }
            try {
                val result = block()
                transaction.forEach { it.commitAsync() }
                result
            } catch (e: Exception) {
                transaction.forEach { it.rollbackAsync() }
                throw e
            }
        }
    }

    suspend fun getEntityManagerAsync(name: String): EntityManager {
        val context = coroutineContext[EntityManagerCoroutineContext] ?: error("在当前协程上下文上不存在数据库上下文！")
        return context.getDataBase(name)
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

    override suspend fun beginTransactionAsync(database: String): DBTransaction {
        return JpaTransaction(getEntityManagerAsync(database).transaction.apply { begin() })
    }

}