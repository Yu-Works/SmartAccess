package com.IceCreamQAQ.SmartAccess.db

import rain.function.dataNode.ObjectNode


object DataSourceUtil{
    fun dbMap(db: ObjectNode, properties: Array<String>): ObjectNode {
        val node = ObjectNode().apply { putAll(db) }
        node.remove("impl")
        if (node.containsKey("url")) {
            val default = ObjectNode()
            node["default"] = default
            properties.forEach { property ->
                node[property]?.let {
                    default[property] = it
                    node.remove(property)
                }
            }
        }
        return node
    }
}