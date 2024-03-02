package com.IceCreamQAQ.SmartAccess.jdbc.pool

import rain.function.dataNode.ObjectNode
import javax.sql.DataSource

interface SupportPool {

    fun createDataSource(name: String, db: ObjectNode): DataSource

}