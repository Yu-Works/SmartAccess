package smartaccess.access.interpreter

import smartaccess.access.query.AbstractQuery

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

        val builder = AbstractQueryBuilder()

        var c = 0
        while (true) {
            if (i >= maxIndex) break
            val word = readWord()

            builder.doWord(
                word,
                { i < maxIndex },
                { readWord() },
                { c = i },
                { i = c }
            )
        }
        return builder.build()
    }

}