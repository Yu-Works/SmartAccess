package smartaccess.hibernate

import org.hibernate.cfg.AvailableSettings
import smartaccess.jpa.JPAService
import smartaccess.jpa.spi.PersistenceUnitInfoImpl
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import rain.classloader.AppClassloader
import javax.inject.Named

@Named("hibernate6")
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
        val ddl = databaseMap[name]?.get("ddl")?.asString() ?: "update"
        config.properties["hibernate.hbm2.auto"] = ddl
        entityManagerFactoryMap[name] = EntityManagerFactoryBuilderImpl(
            PersistenceUnitInfoDescriptor(config),
            HashMap<String, Any>()
                .apply {
                    put(AvailableSettings.CLASSLOADERS, listOf(appClassloader))
                    put(AvailableSettings.TC_CLASSLOADER, appClassloader)
                },
            appClassloader
        ).build()
    }
}