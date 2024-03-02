package com.IceCreamQAQ.SmartAccess.reflect

import rain.function.toUpperCaseFirstOne

interface FieldReader<E, F> {
    operator fun invoke(entity: E): F?

    companion object {
        fun <E, R> make(clazz: Class<E>, fieldName: String): FieldReader<E, R> {
            val field = clazz.getDeclaredField(fieldName)
            val setter = runCatching { clazz.getDeclaredMethod("set${fieldName.toUpperCaseFirstOne()}") }
                .getOrNull()

            TODO()
        }
    }
}