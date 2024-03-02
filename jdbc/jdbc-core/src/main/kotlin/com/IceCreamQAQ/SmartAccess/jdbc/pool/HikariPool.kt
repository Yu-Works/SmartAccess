package com.IceCreamQAQ.SmartAccess.jdbc.pool

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import rain.function.dataNode.ObjectNode

object HikariPool:SupportPool {

    override fun createDataSource(name: String, db: ObjectNode): HikariDataSource {
        val hc = HikariConfig()

        hc.transactionIsolation = "TRANSACTION_READ_COMMITTED"
        hc.jdbcUrl = db["url"]?.asString() ?: error("数据库 $name 配置错误，缺少 url。")
        hc.username = db["username"]?.asString() ?: error("数据库 $name 配置错误，缺少 username。")
        hc.password = db["password"]?.asString() ?: error("数据库 $name 配置错误，缺少 password。")
        hc.driverClassName = db["driver"]?.asString() ?: error("数据库 $name 配置错误，缺少 driver。")
        hc.maximumPoolSize = db["poolMax"]?.asInt() ?: 10
        hc.minimumIdle = db["poolIdle"]?.asInt() ?: 2
        hc.connectionTimeout = 30 * 1000
        hc.isAutoCommit = false
        hc.isReadOnly = false

        return HikariDataSource(hc)
    }

}