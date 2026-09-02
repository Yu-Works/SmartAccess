package smartaccess.access.interpreter

import smartaccess.access.interpreter.stage.EndStage
import smartaccess.access.interpreter.stage.FunctionStage
import smartaccess.access.interpreter.stage.OrderByStage
import smartaccess.access.interpreter.stage.ParamStage
import smartaccess.access.interpreter.stage.SqlStage
import smartaccess.access.interpreter.stage.WhereStage
import smartaccess.access.query.AbstractQuery

class AbstractQueryBuilder {

    val function = FunctionStage()
    val param = ParamStage()
    val where = WhereStage()
    val orderBy = OrderByStage()

    var forUpdate: Boolean = false
    var start: Int? = null
    var limit: Int? = null

    var currentStage: SqlStage = function
    fun changeStage(stage: SqlStage) {
        currentStage.endStage()
        currentStage = stage
    }


    fun doWord(word: String, hasNext: () -> Boolean, nextWord: () -> String, mark: () -> Unit, revoke: () -> Unit) {
        if (word == "By" && currentStage == param) return changeStage(where)

        if (word == "Order" && hasNext()) {
            mark()
            if (nextWord() == "By") return changeStage(orderBy)
            else revoke()
        }

        if (word == "For" && hasNext()) {
            mark()
            if (nextWord() == "Update") {
                forUpdate = true
                return changeStage(EndStage())
            } else revoke()
        }

        if (word.startsWith("Limit") && word.length > 5 && !hasNext()) {
            val limitStr = word.substring(5)
            limit = limitStr.toIntOrNull() ?: error("Limit后面必须是数字。")
            return changeStage(EndStage())
        }

        currentStage.doWord(word)
        if (currentStage == function) changeStage(param)
    }

    fun build(): AbstractQuery {
        currentStage.endStage()
        return AbstractQuery(
            function.function ?: error("没有操作方法名。"),
            param.params,
            where.wheres,
            orderBy.orderBys,
            forUpdate,
            start,
            limit,
        )
    }


}