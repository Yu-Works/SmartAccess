package com.IceCreamQAQ.SmartAccess.hibernate.test

import com.IceCreamQAQ.SmartAccess.annotation.Transactional
import com.IceCreamQAQ.SmartAccess.hibernate.test.model.StudentAccess
import com.IceCreamQAQ.SmartAccess.item.Page
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
    @Transactional
    fun AppStartEvent.onEvent() {
//        studentAccess.save(Student(name = "2", age = 1))
        println(studentAccess.findByAgeOrderByName(1, Page(0,1)))
    }

}

fun main() {
    FullStackApplicationLauncher.launch()
}