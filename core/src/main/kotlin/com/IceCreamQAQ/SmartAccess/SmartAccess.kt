package com.IceCreamQAQ.SmartAccess

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import javax.inject.Named

class SmartAccess(
    @Named("{db.impl}")
    val dbService: DBService
) : ApplicationService {


    override fun init() {
        dbService.start()
    }

    override fun start() {}

    override fun stop() {
        dbService.close()
    }
}