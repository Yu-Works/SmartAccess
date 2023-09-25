package com.IceCreamQAQ.SmartAccess.db.transaction

import com.IceCreamQAQ.SmartAccess.SmartAccess

class TransactionService(
    private val sa: SmartAccess
) {

    private val transactionContext: ThreadLocal<TransactionContext?> = ThreadLocal()

    fun beginTransactionSync(databases: Array<String>): TransactionContext? {
        if (transactionContext.get() != null) return null
        return TransactionContext(
            "",
            HashMap<String, DBTransaction>().apply {
                databases.forEach {
                    put(it, sa.dbServiceMap[it]?.context?.beginTransactionSync(it) ?: error("数据库 $it 不存在"))
                }
            }
        )
    }


    suspend fun beginTransactionAsync(databases: Array<String>): TransactionContext? =
        TransactionContext(
            "",
            HashMap<String, DBTransaction>().apply {
                databases.forEach {
                    put(it, sa.dbServiceMap[it]?.context?.beginTransactionAsync(it) ?: error("数据库 $it 不存在"))
                }
            }
        )

}