package com.IceCreamQAQ.SmartAccess.access

import com.IceCreamQAQ.SmartAccess.annotation.ProvideAccessTemple
import rain.api.annotation.LoadBy

@ProvideAccessTemple
@LoadBy(AccessLoader::class, mastBean = false)
interface Access<T, PK>