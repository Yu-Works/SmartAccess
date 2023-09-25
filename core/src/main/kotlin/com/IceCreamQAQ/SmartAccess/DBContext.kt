package com.IceCreamQAQ.SmartAccess

import com.IceCreamQAQ.SmartAccess.db.transaction.DBTransaction

interface DBContext{
    fun beginTransactionSync(database: String): DBTransaction
    fun beginTransactionAsync(database: String): DBTransaction
}