package com.IceCreamQAQ.SmartAccess.hibernate.test.model

import smartaccess.item.Page
import smartaccess.jpa.access.JpaAccess
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import smartaccess.item.PageResult
import smartaccess.jpa.access.JpaAsyncAccess

@Entity
data class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,
    var name: String = "",
    var age: Int = 0
)

interface StudentAccess : JpaAsyncAccess<Student, Int> {
    suspend fun findByName(name: String, page: Page = Page(1,2)): PageResult<Student>
    suspend fun deleteByName(name: String): Int
//    fun findByAge(age: Int): List<Student>
//    fun findByAgeOrderByName(age: Int, page: Page): List<Student>
//    fun text() = "World"
//    fun print(text:String? = "world"){
//        println("Hello $text!")
//    }
//    fun countByAge(age: Int): Long
}