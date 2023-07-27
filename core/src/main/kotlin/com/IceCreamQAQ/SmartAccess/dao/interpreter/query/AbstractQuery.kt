package com.IceCreamQAQ.SmartAccess.dao.interpreter.query

class AbstractQuery(
    val queryType: Int,
    val optParams: List<String>,
    val wheres: List<WhereItem>,
    val orderBys: List<OrderByItem>
) {
    override fun toString(): String {
        val query = StringBuilder()
        when (queryType) {
            1 -> query.append("select ${if (optParams.isEmpty()) "*" else optParams.joinToString(", ")} from")
            2 -> query.append("select count(${if (optParams.isEmpty()) "*" else optParams[0]}) from")
            5 -> query.append("update set").apply { optParams.joinToString(", ") { "$it = ?" } }
            8 -> query.append("delete from")
        }
        if (wheres.isNotEmpty()) {
            query.append(" where")
            wheres.forEach {
                query.append(" ${it.key} ${it.operator}")
                if (it.needValue) query.append(" ?")
                it.infix?.let { infix -> query.append(" ").append(infix) }
            }
        }
        if (orderBys.isNotEmpty()) {
            query.append(" order by ")
            query.append(orderBys.joinToString(", ") { "${it.key} ${it.sort}" })
        }
        return query.append(";").toString()
    }
}