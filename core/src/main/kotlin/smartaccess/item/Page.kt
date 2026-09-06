package smartaccess.item

data class Page(
    val start: Int = 0,
    val num: Int = 10
) {

    companion object {
        val single = Page(0, 1)

        fun page(pIndex: Int, pSize: Int) = Page((pIndex - 1) * pSize, pSize)
    }

}