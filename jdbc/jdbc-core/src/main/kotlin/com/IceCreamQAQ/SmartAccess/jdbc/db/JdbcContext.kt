package com.IceCreamQAQ.SmartAccess.jdbc.db

import com.IceCreamQAQ.SmartAccess.jdbc.JdbcService
import java.sql.Connection

class JdbcContext(private val service: JdbcService) {

    private val ctl = HashMap<String, ThreadLocal<Connection>>(service.connectMap.size)

    init {
        for (key in service.connectMap.keys) {
            ctl[key] = ThreadLocal()
        }
    }

    fun getConnection(name: String): Connection {
        var con = (ctl[name] ?: error("数据库 $name 上下文不存在！请检查数据库配置！")).get()
        if (con != null && !con.isClosed) return con

        con = service.connectMap[name]!!.connection
        ctl[name]!!.set(con)
        return con
    }

    fun closeConnection(name: String) {
        val con = (ctl[name] ?: error("数据库 $name 上下文不存在！请检查数据库配置！")).get() ?: return
        if (!con.isClosed) con.close()
        ctl[name]!!.remove()
    }
}