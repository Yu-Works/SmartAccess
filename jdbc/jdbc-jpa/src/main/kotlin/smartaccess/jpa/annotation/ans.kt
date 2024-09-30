package smartaccess.jpa.annotation

import jakarta.persistence.LockModeType
import smartaccess.jpa.access.QueryRewriter
import kotlin.reflect.KClass

annotation class NativeQuery

annotation class Lock(val value: LockModeType = LockModeType.NONE)

annotation class SearchRewriter(val value: KClass<out QueryRewriter>)
annotation class ExecuteRewriter(val value: KClass<out QueryRewriter>)
