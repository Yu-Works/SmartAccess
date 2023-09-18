package com.IceCreamQAQ.SmartAccess.access

import java.lang.reflect.ParameterizedType

interface AccessMetadataProvider {

    fun getAccessModelType(accessClass: Class<out Access<*, *>>): Class<*>
    fun getAccessPrimaryKeyType(accessClass: Class<out Access<*, *>>): Class<*>

    object Default : AccessMetadataProvider {

        override fun getAccessModelType(accessClass: Class<out Access<*, *>>): Class<*> {
            val type = accessClass.genericInterfaces[0] as ParameterizedType
            return type.actualTypeArguments[0] as Class<*>
        }

        override fun getAccessPrimaryKeyType(accessClass: Class<out Access<*, *>>): Class<*> {
            val type = accessClass.genericInterfaces[0] as ParameterizedType
            return type.actualTypeArguments[1] as Class<*>
        }

    }

}