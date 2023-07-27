package com.test

import com.IceCreamQAQ.SmartAccess.dao.interpreter.MethodInterpreter

fun main() {
    val queries = arrayOf(
        "findByUsernameIsnAndPasswordIsrOrderByUsernameDesc",
        "countByUsername",
        "updateNameAndAgeByUsername",
    )

    queries.forEach { query ->
        println(MethodInterpreter(query))
    }
}