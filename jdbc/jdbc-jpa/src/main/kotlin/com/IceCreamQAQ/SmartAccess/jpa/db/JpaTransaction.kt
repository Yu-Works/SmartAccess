package com.IceCreamQAQ.SmartAccess.jpa.db

import com.IceCreamQAQ.SmartAccess.db.transaction.DBTransaction
import jakarta.persistence.EntityTransaction

class JpaTransaction(private val transaction: EntityTransaction): DBTransaction {
    override fun commitSync() {
        transaction.commit()
    }

    override suspend fun commitAsync() {
        transaction.commit()
    }

    override fun rollbackSync() {
        transaction.rollback()
    }

    override suspend fun rollbackAsync() {
        transaction.rollback()
    }
}