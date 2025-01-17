package smartaccess.item

data class PageResult<T>(
    val total: Long,
    val data: List<T>
)