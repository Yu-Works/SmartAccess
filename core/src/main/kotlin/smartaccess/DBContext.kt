package smartaccess

import smartaccess.db.transaction.DBTransaction

interface DBContext{
    fun beginTransactionSync(database: String): DBTransaction
    suspend fun beginTransactionAsync(database: String): DBTransaction
}