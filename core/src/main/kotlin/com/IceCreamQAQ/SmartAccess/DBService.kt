package com.IceCreamQAQ.SmartAccess

import java.io.Closeable

interface DBService : Closeable {
    fun start()
}