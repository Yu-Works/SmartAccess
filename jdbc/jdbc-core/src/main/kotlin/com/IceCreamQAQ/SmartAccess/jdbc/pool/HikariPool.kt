package com.IceCreamQAQ.SmartAccess.jdbc.pool

import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object HikariPool:SupportPool {

    override fun createDataSource(name: String, db: ObjectNode): HikariDataSource {
        val hc = HikariConfig()

        hc.transactionIsolation = "TRANSACTION_READ_COMMITTED"
        hc.jdbcUrl = db["url"]?.asString() ?: error("数据库 $name 配置错误，缺少 url。")
        hc.username = db["username"]?.asString() ?: error("数据库 $name 配置错误，缺少 username。")
        hc.password = db["password"]?.asString() ?: error("数据库 $name 配置错误，缺少 password。")
        hc.driverClassName = db["driver"]?.asString() ?: error("数据库 $name 配置错误，缺少 driver。")
        hc.maximumPoolSize = db["poolMax"]?.asInt() ?: error("数据库 $name 配置错误，缺少 poolMax。")
        hc.minimumIdle = db["poolIdle"]?.asInt() ?: error("数据库 $name 配置错误，缺少 poolIdle。")
        hc.connectionTimeout = 30 * 1000
        hc.isAutoCommit = false
        hc.isReadOnly = false

        return HikariDataSource(hc)
    }

}