package com.IceCreamQAQ.SmartAccess.hibernate

import com.IceCreamQAQ.SmartAccess.jpa.JPAService
import com.IceCreamQAQ.SmartAccess.jpa.spi.PersistenceUnitInfoImpl
import com.IceCreamQAQ.Yu.loader.AppClassloader
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import javax.inject.Named

@Named("hibernate5")
class HibernateService(
    @Named("appClassloader") appClassloader: ClassLoader
) : JPAService(appClassloader as AppClassloader) {
    override fun startDatabase(name: String, models: List<Class<*>>) {
        val config = PersistenceUnitInfoImpl(
            "org.hibernate.jpa.HibernatePersistenceProvider",
            databaseMap[name]!!,
            false,
            dataSourceMap[name]!!,
            appClassloader,
            models
        )
        config.properties["hibernate.hbm2ddl.auto"] = "update"
        entityManagerFactoryMap[name] = EntityManagerFactoryBuilderImpl(
            PersistenceUnitInfoDescriptor(config),
            HashMap<String,Any>()
        ).build()
    }
}