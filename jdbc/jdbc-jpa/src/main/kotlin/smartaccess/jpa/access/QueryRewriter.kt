package smartaccess.jpa.access

fun interface QueryRewriter {
    operator fun invoke(query: String): String
}