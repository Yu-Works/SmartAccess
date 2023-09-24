package com.IceCreamQAQ.SmartAccess.hibernate.test

import com.IceCreamQAQ.SmartAccess.hibernate.test.model.StudentAccess
import com.IceCreamQAQ.Yu.FullStackApplicationLauncher
import com.IceCreamQAQ.Yu.annotation.Event
import com.IceCreamQAQ.Yu.annotation.EventListener
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import javax.inject.Inject

@EventListener
class TestListener {

    @Inject
    lateinit var studentAccess : StudentAccess

    @Event
    fun AppStartEvent.onEvent() {
        println(studentAccess.findAll())
    }

}

fun main() {
    FullStackApplicationLauncher.launch()
}