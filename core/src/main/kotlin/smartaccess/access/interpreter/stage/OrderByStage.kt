package smartaccess.access.interpreter.stage

import smartaccess.access.query.OrderByItem

class OrderByStage: SqlStage {
    val orderBys = ArrayList<OrderByItem>()
    val nameBuilder = StringBuilder()

    fun endItem(sort: String = "ASC") {
        if (nameBuilder.isNotEmpty()) {
            orderBys.add(OrderByItem(nameBuilder.toString(), sort))
            nameBuilder.clear()
        }
    }

    override fun doWord(word: String) {
        val sort = when (word) {
            "Asc" -> "ASC"
            "Desc" -> "DESC"
            "And" -> "ASC"
            else -> null
        }

        if (sort == null) nameBuilder.append(word)
        else endItem(sort)
    }

    override fun endStage() {
        endItem()
    }

}