package com.IceCreamQAQ.SmartAccess.access

import com.IceCreamQAQ.SmartAccess.annotation.ProvideAccessTemple
import com.IceCreamQAQ.Yu.annotation.LoadBy
import java.lang.reflect.ParameterizedType

@ProvideAccessTemple
@LoadBy(AccessLoader::class)
interface Access<T, PK>