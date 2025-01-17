package com.IceCreamQAQ.SmartAccess.hibernate.test

import com.IceCreamQAQ.SmartAccess.hibernate.test.model.Student
import com.IceCreamQAQ.SmartAccess.hibernate.test.model.StudentAccess
import kotlinx.coroutines.runBlocking
import rain.application.FullStackApplicationLauncher
import rain.application.events.AppStatusEvent
import rain.event.annotation.EventListener
import rain.event.annotation.SubscribeEvent
import rain.function.cast
import smartaccess.SmartAccess
import smartaccess.annotation.Transactional
import smartaccess.jpa.JPAService
import smartaccess.jpa.db.JpaContext
import javax.inject.Inject

@EventListener
class TestListener(
    val sa: SmartAccess
) {

    @Inject
    lateinit var studentAccess: StudentAccess

    @SubscribeEvent
    fun AppStatusEvent.AppStarted.onEvent() {
//        studentAccess.save(Student(name = "2", age = 1))
//        studentAccess.print()
        runBlocking {
            sa.defaultService?.cast<JPAService>()?.context?.transaction {
//                studentAccess.save(Student(name = "1", age = 101))
//                studentAccess.save(Student(name = "1", age = 102))
//                studentAccess.save(Student(name = "1", age = 103))
//                studentAccess.save(Student(name = "1", age = 104))
//                studentAccess.save(Student(name = "1", age = 105))
//                studentAccess.save(Student(name = "1", age = 106))
//                studentAccess.save(Student(name = "1", age = 107))
//                studentAccess.save(Student(name = "1", age = 108))
                println(studentAccess.deleteByName("2"))
//                println(studentAccess.findAll())
            }
        }
    }

}

fun main() {
    FullStackApplicationLauncher.launch()
}