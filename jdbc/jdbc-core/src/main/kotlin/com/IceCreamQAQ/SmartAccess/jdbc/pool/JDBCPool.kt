package com.IceCreamQAQ.SmartAccess.jdbc.pool

import com.IceCreamQAQ.Yu.mapMap
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import javax.sql.DataSource

object JDBCPool {

    val supportPools = mutableMapOf(
        "HikariCP" to ("com.zaxxer.hikari.HikariDataSource" to HikariPool)
    )

    fun createConnectMap(dbMap: ObjectNode): Map<String, DataSource> {
        supportPools.values.forEach { (clazz, creator) ->
            runCatching {
                Class.forName(clazz)
                return dbMap.mapMap { (name, db) ->
                    name to creator.createDataSource(
                        name,
                        db as? ObjectNode ?: error("数据库 $name 配置错误，数据库信息转化失败！")
                    )
                }
            }
        }
        error("没有引入任何受支持的连接池类型！请引入以下连接池之一：${supportPools.keys.joinToString(", ")}")
    }

}