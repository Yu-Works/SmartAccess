package com.IceCreamQAQ.SmartAccess.access

import com.IceCreamQAQ.SmartAccess.SmartAccess
import com.IceCreamQAQ.SmartAccess.annotation.Database
import com.IceCreamQAQ.SmartAccess.annotation.MetadataProvider
import com.IceCreamQAQ.SmartAccess.annotation.ProvideAccessTemple
import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.hasAnnotation
import com.IceCreamQAQ.Yu.loader.LoadItem
import com.IceCreamQAQ.Yu.loader.Loader

class AccessLoader(
    val context: YuContext,
    val sa: SmartAccess
) : Loader {

    override fun load(items: Collection<LoadItem>) {
        items.forEach {
            val accessClass = it.clazz as Class<out Access<*, *>>

            if (!accessClass.isInterface) return@forEach
            if (accessClass.hasAnnotation<ProvideAccessTemple>()) return@forEach

            val metadataProvider =
                findMetadataProvider(accessClass, ArrayList()).let { providers ->
                    when {
                        providers.isEmpty() -> AccessMetadataProvider.Default
                        providers.size == 1 -> context.getBean(providers[0])
                            ?: error("Access ${accessClass.name} 提供了 MetadataProvider 声明 ${providers[0].name}，但是没有找到或是无法创建该类的实例。")

                        else -> throw RuntimeException("Access $accessClass 拥有 MetadataProvider 提供者声明，请检查 Access 继承传递链。")
                    }
                }

            val modelType = metadataProvider.getAccessModelType(accessClass)
            val database = modelType.annotation<Database>()?.value ?: "default"

            val dbService = sa.dbServiceMap[database]
                ?: error("Access ${accessClass.name} 指定的数据库 $database 不存在！请重新确认数据库配置信息！")

            val access = dbService.createAccess(accessClass, modelType, metadataProvider)
            context.putBean(accessClass as Class<Any>, access)
        }
    }

    private fun findMetadataProvider(
        clazz: Class<*>,
        providers: MutableList<Class<out AccessMetadataProvider>>
    ): List<Class<out AccessMetadataProvider>> {
        clazz.annotation<MetadataProvider> { providers.add(value.java) }
        clazz.interfaces.forEach { findMetadataProvider(it, providers) }
        return providers
    }

}