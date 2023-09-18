package com.IceCreamQAQ.SmartAccess.access

import com.IceCreamQAQ.SmartAccess.reflect.FieldReader

abstract class AbstractAccess<T, PK> : Access<T, PK> {

    val entityType: Class<T> = getModelType()
    val primaryKeyType: Class<PK> = getPrimaryKeyType()

    abstract val primaryKeyReader: FieldReader<T, PK>


}