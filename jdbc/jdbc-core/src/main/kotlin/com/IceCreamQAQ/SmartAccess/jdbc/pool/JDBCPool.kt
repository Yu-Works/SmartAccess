package com.IceCreamQAQ.SmartAccess.jdbc.pool

object JDBCPool {

    private val supportPools = mutableMapOf(
        "HikariCP" to ("com.zaxxer.hikari.HikariDataSource" to HikariPool)
    )

    lateinit var supportPool: SupportPool
        private set

    init {
        for (it in supportPools.values) {
            try {
                Class.forName(it.first)
                supportPool = it.second
            } catch (_: Exception) {
            }
        }
        error("没有引入任何受支持的连接池类型！请引入以下连接池之一：${supportPools.keys.joinToString(", ")}")
    }

}