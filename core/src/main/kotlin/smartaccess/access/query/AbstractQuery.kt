package smartaccess.access.query

class AbstractQuery(
    val queryType: Int,
    val optParams: List<String>,
    val wheres: List<WhereItem>,
    val orderBys: List<OrderByItem>
) {
    fun toSqlString(
        table: String,
        needSelect: Boolean = true,
        needIndex: Boolean = false,
        needName: Boolean = false
    ): String {
        val query = StringBuilder()
        when (queryType) {
            1 -> query.apply {
                if (optParams.isNotEmpty() || needSelect)
                    append("select ${if (optParams.isEmpty()) "*" else optParams.joinToString(", ")} ")
            }.append("from")

            2 -> query.append("select count(${if (optParams.isEmpty()) "*" else optParams[0]}) from")
            5 -> query.append("update set").apply { optParams.joinToString(", ") { "$it = ?" } }
            8 -> query.append("delete from")
        }
        query.append(' ').append(table)
        if (wheres.isNotEmpty()) {
            query.append(" where")
            var paramIndex = 0
            wheres.forEach {
                query.append(" ${it.key} ${it.operator}")
                if (it.needValue)
                    if (needName) query.append(" :${it.key}")
                    else {
                        query.append(" ?")
                        if (needIndex) query.append(paramIndex++)
                    }
                it.infix?.let { infix -> query.append(" ").append(infix) }
            }
        }
        if (orderBys.isNotEmpty()) {
            query.append(" order by ")
            query.append(orderBys.joinToString(", ") { "${it.key} ${it.sort}" })
        }
        return query.toString()
    }
}