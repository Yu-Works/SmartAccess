package smartaccess.access.interpreter

import rain.function.toLowerCaseFirstOne
import smartaccess.access.query.AbstractQuery
import smartaccess.access.query.OrderByItem
import smartaccess.access.query.WhereItem

object MethodInterpreter {

    operator fun invoke(name: String): AbstractQuery {
        val maxIndex = name.length - 1
        var i = 0

        fun next(value: String): Boolean {
            val max = i + value.length
            if (max > maxIndex) return false

            if (name.substring(i, max) == value) {
                i = max
                return true
            }
            return false
        }

        fun readWord(): String {
            val start = i
            var f = false
            while (i <= maxIndex) {
                val c = name[i]
                if (c.isUpperCase()) if (f) break
                f = true
                i++
            }
            return name.substring(start, i)
        }

        val type = when (readWord()) {
            "find" -> 1
            "count" -> 2
            "update" -> 5
            "delete" -> 8
            else -> error("未能解析的方法名: $name。")
        }
        val nameBuilder = StringBuilder()

        val optParams = ArrayList<String>()
        apply {

            fun endName() {
                if (nameBuilder.isNotEmpty()) {
                    optParams.add(nameBuilder.toString())
                    nameBuilder.clear()
                }
            }

            while (true) {
                if (i >= maxIndex) break

                when (val word = readWord()) {
                    "By" -> break
                    "And" -> endName()
                    else -> nameBuilder.append(word)
                }
            }
            endName()

            if (type == 2 && optParams.size > 1) error("count 方法不能接受过多的参数: $optParams。")
            if (type == 5 && optParams.isEmpty()) error("update 方法必须接受至少一个参数。")
            if (type == 8 && optParams.isNotEmpty()) error("delete 方法不能接受参数。")
        }

        val wheres = ArrayList<WhereItem>()
        apply {
            var expression = "=" to true

            fun endItem(infix: String? = null) {
                if (nameBuilder.isEmpty()) return
                wheres.add(WhereItem(nameBuilder.toString().toLowerCaseFirstOne(), expression.first, expression.second, infix))
                expression = "=" to true
                nameBuilder.clear()
            }

            while (true) {
                if (i >= maxIndex) break
                val word = readWord()

                if (word == "Order") {
                    val ci = i
                    if (readWord() == "By") break
                    else i = ci
                }

                if (word == "And" || word == "Or") {
                    endItem(word)
                    continue
                }

                val infix = when (word) {
                    "Isn" -> "IS NULL" to false
                    "Isr" -> "IS NOT NULL" to false
                    "Like" -> "LIKE" to true
                    "In" -> "IN" to true
                    "Ne" -> "!=" to true
                    "Nl" -> "NOT LIKE" to true
                    "Nin" -> "NOT IN" to true
                    "Gt" -> ">" to true
                    "Lt" -> "<" to true
                    "Gte" -> ">=" to true
                    "Lte" -> "<=" to true
                    else -> null
                }

                if (infix == null) nameBuilder.append(word)
                else expression = infix
            }

            endItem()
        }

        val orderBys = ArrayList<OrderByItem>()
        apply {
            fun endItem(sort: String = "ASC") {
                if (nameBuilder.isNotEmpty()) {
                    orderBys.add(OrderByItem(nameBuilder.toString(), sort))
                    nameBuilder.clear()
                }
            }

            while (true) {
                if (i >= maxIndex) break
                val word = readWord()

                val sort = when (word) {
                    "Asc" -> "ASC"
                    "Desc" -> "DESC"
                    "And" -> "ASC"
                    else -> null
                }

                if (sort == null) nameBuilder.append(word)
                else endItem(sort)
            }
            endItem()
        }

        return AbstractQuery(type, optParams, wheres, orderBys)
    }

}