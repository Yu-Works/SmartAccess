package smartaccess.annotation

import smartaccess.access.AccessMetadataProvider
import rain.hook.HookBy
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Model

@Target(AnnotationTarget.CLASS)
annotation class Database(val value: String = "default")

@Target(AnnotationTarget.CLASS)
annotation class MetadataProvider(val value: KClass<out AccessMetadataProvider>)


annotation class ProvideAccessTemple

@HookBy("smartaccess.db.transaction.TransactionHook")
annotation class Transactional(val dbList: Array<String> = ["default"])