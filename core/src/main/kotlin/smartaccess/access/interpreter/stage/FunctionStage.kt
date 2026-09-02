package smartaccess.access.interpreter.stage

class FunctionStage: SqlStage {

    var function: Int? = null

    override fun doWord(word: String) {
        function = when (word) {
            "find" -> 1
            "count" -> 2
            "update" -> 5
            "delete" -> 8
            else -> error("未能解析的方法名: $word。")
        }
    }

    override fun endStage() {
        if (function == null) error("没有操作方法名。")
    }
}