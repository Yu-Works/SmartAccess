package com.IceCreamQAQ.SmartAccess.jdbc

import com.IceCreamQAQ.SmartAccess.DBService
import com.IceCreamQAQ.SmartAccess.access.Access
import com.IceCreamQAQ.SmartAccess.db.DataSourceUtil
import com.IceCreamQAQ.SmartAccess.jdbc.pool.JDBCPool
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import javax.inject.Named
import javax.sql.DataSource


@Named("JDBC")
class JdbcService(@Config db: ObjectNode) : DBService {

    val dbMap = DataSourceUtil.dbMap(
        db,
        arrayOf(
            "username",
            "password",
            "driver",
            "poolMax",
            "poolIdle"
        )
    )
    lateinit var connectMap: Map<String, DataSource>

    override fun start() {
        connectMap = JDBCPool.createConnectMap(dbMap)
    }

    override fun makeAccess(accessInterface: Class<out Access<*, *>>) {
        TODO("Not yet implemented")
    }

    override fun close() {
        if (::connectMap.isInitialized) connectMap.values.forEach { it.connection.close() }
    }
}