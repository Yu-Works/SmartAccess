package smartaccess.jdbc.access

import smartaccess.item.Page
import rain.api.annotation.AutoBind

@AutoBind
interface JDBCPageAble {
    operator fun invoke(sql: String, page: Page): String
}