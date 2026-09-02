package smartaccess.access.interpreter.stage

import rain.function.toLowerCaseFirstOne
import smartaccess.access.query.WhereItem

class WhereStage: SqlStage{
    val wheres = ArrayList<WhereItem>()
    val nameBuilder = StringBuilder()
    var expression = "=" to true

    fun endItem(infix: String? = null) {
        if (nameBuilder.isEmpty()) return
        wheres.add(WhereItem(nameBuilder.toString().toLowerCaseFirstOne(), expression.first, expression.second, infix))
        expression = "=" to true
        nameBuilder.clear()
    }

    override fun doWord(word: String) {
        if (word == "And" || word == "Or") {
            endItem(word)
            return
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

    override fun endStage() {
        endItem()
    }

}