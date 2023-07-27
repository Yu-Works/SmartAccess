package com.IceCreamQAQ.SmartAccess.dao

import com.IceCreamQAQ.SmartAccess.item.Page
import com.IceCreamQAQ.Yu.annotation.LoadBy
import java.io.Serializable
import java.lang.reflect.ParameterizedType

@LoadBy(DaoLoader::class)
interface Access<T, PK : Serializable> {

    fun getEntityType(): Class<T> =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

    fun getPrimaryKeyType(): Class<PK> =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<PK>

    fun get(id: PK): T?
    fun delete(id: PK)

    fun save(entity: T)
    fun update(entity: T)

    fun saveOrUpdate(entity: T)

    fun where(paras: Map<String, Any>)
    fun where(paras: Map<String, Any>, page: Page)

}