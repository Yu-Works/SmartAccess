package com.IceCreamQAQ.SmartAccess.jdbc.pool

import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import javax.sql.DataSource

interface SupportPool {

    fun createDataSource(name: String, db: ObjectNode): DataSource

}