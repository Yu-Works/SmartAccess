package smartaccess.access.interpreter.stage

interface SqlStage {
    fun doWord(word: String)
    fun endStage()
}