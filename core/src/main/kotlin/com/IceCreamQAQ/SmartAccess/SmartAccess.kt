package com.IceCreamQAQ.SmartAccess

import com.IceCreamQAQ.SmartAccess.annotation.Database
import com.IceCreamQAQ.SmartAccess.annotation.Model
import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.ClassRegister
import com.IceCreamQAQ.Yu.util.dataNode.ObjectNode
import com.IceCreamQAQ.Yu.util.dataNode.StringNode

class SmartAccess(
    @Config("db") dbNode: ObjectNode,
    val context: YuContext
) : ApplicationService, ClassRegister {

    val dbServiceMap: Map<String, DBService>
    private val dbModuleMap = HashMap<String, MutableList<Class<*>>>()

    val defaultService: DBService?

    init {
        val dbServiceMap = HashMap<String, DBService>()
        val dbConfigMap = HashMap<String, ObjectNode>()
        val defaultDatabase = ObjectNode()

        val defaultProvider = dbNode["provider"]?.asString()

        dbNode.forEach { (key, value) ->
            if (value is StringNode) defaultDatabase[key] = value
            else dbConfigMap[key] = value as ObjectNode
        }

        if (defaultDatabase.size() > 1) dbConfigMap["default"] = defaultDatabase

        dbConfigMap.forEach { (key, value) ->
            val provider = value["provider"]?.asString()
                ?: defaultProvider
                ?: error("数据库 $key 没有配置对应的服务提供者，且未提供公共提供者配置项（db.provider）。")

            val dbService = context.getBean(DBService::class.java, provider)
                ?: error("数据库 $key 指定的服务提供者 $provider 不存在或无法创建。")
            dbServiceMap[key] = dbService

            dbService.initDatabase(key, value)
        }

        defaultService = dbServiceMap["default"]
        this.dbServiceMap = dbServiceMap
    }

    override fun register(clazz: Class<*>) {
        clazz.annotation<Database> {
            dbModuleMap.getOrPut(value) { ArrayList() }.add(clazz)
            return
        }
        clazz.annotation<Model> {
            dbModuleMap.getOrPut("default") { ArrayList() }.add(clazz)
            return
        }
        if (defaultService?.isModel(clazz) == true) {
            dbModuleMap.getOrPut("default") { ArrayList() }.add(clazz)
        }
    }

    override fun init() {
        dbServiceMap.forEach { (name, service) ->
            service.startDatabase(name, dbModuleMap[name] ?: ArrayList())
        }
    }

    override fun start() {}

    override fun stop() {
        dbServiceMap.forEach { (_, service) ->
            service.close()
        }
    }


}