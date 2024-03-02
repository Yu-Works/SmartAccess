package com.IceCreamQAQ.SmartAccess.jdbc.access

import com.IceCreamQAQ.SmartAccess.item.Page
import rain.api.annotation.AutoBind

@AutoBind
interface JDBCPageAble {
    operator fun invoke(sql: String, page: Page): String
}