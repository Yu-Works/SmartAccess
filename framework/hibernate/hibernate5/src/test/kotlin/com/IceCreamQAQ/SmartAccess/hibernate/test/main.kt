package com.IceCreamQAQ.SmartAccess.hibernate.test

import com.IceCreamQAQ.SmartAccess.hibernate.test.model.StudentAccess
import rain.application.FullStackApplicationLauncher
import rain.application.events.AppStatusEvent
import rain.event.annotation.EventListener
import rain.event.annotation.SubscribeEvent
import smartaccess.annotation.Transactional
import javax.inject.Inject

@EventListener
class TestListener {

    @Inject
    lateinit var studentAccess : StudentAccess

    @SubscribeEvent
    @Transactional
    fun AppStatusEvent.AppStarted.onEvent() {
//        studentAccess.save(Student(name = "2", age = 1))
//        studentAccess.print()
        println(studentAccess.countByAge(1))
    }

}

fun main(){
    FullStackApplicationLauncher.launch()
}