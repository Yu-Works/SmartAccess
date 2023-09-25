package com.IceCreamQAQ.SmartAccess.db.transaction

interface DBTransaction {

    fun commitSync()
    suspend fun commitAsync()
    fun rollbackSync()
    suspend fun rollbackAsync()

}