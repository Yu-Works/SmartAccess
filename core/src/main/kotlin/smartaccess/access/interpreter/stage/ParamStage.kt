package smartaccess.access.interpreter.stage

class ParamStage: SqlStage {
    val params = ArrayList<String>()
    val nameBuilder = StringBuilder()

    fun endItem() {
        if (nameBuilder.isNotEmpty()) {
            params.add(nameBuilder.toString())
            nameBuilder.clear()
        }
    }

    override fun doWord(word: String) {
        if (word == "And") endItem()
        else nameBuilder.append(word)
    }

    override fun endStage() {
        endItem()
    }
}