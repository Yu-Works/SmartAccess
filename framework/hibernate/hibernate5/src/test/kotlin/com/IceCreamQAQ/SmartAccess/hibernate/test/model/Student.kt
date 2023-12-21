package com.IceCreamQAQ.SmartAccess.hibernate.test.model

import com.IceCreamQAQ.SmartAccess.item.Page
import com.IceCreamQAQ.SmartAccess.jpa.access.JpaAccess
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var name: String = "",
    var age: Int = 0
)

interface StudentAccess : JpaAccess<Student, Int> {
    fun findByName(name: String): Student?
    fun findByAge(age: Int): List<Student>
    fun findByAgeOrderByName(age: Int, page: Page): List<Student>
    fun text() = "World"
    fun print(text:String? = "world"){
        println("Hello $text!")
    }
    fun countByAge(age: Int): Long
}