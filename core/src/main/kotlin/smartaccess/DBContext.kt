package smartaccess

import smartaccess.db.transaction.DBTransaction

interface DBContext{
    fun beginTransactionSync(database: String): DBTransaction
    fun beginTransactionAsync(database: String): DBTransaction
}