package com.IceCreamQAQ.SmartAccess.annotation

import com.IceCreamQAQ.SmartAccess.access.AccessMetadataProvider
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Model

@Target(AnnotationTarget.CLASS)
annotation class Database(val value: String = "default")

@Target(AnnotationTarget.CLASS)
annotation class MetadataProvider(val value: KClass<out AccessMetadataProvider>)


annotation class ProvideAccessTemple