package smartaccess.db.transaction

class TransactionContext(
    val actionId: String,
    val transactions: Map<String, DBTransaction>
) {

    fun commitSync() {
        transactions.values.forEach { it.commitSync() }
    }

    suspend fun commitAsync() {
        transactions.values.forEach { it.commitAsync() }
    }

    fun rollbackSync() {
        transactions.values.forEach { it.rollbackSync() }
    }

    suspend fun rollbackAsync() {
        transactions.values.forEach { it.rollbackAsync() }
    }

}