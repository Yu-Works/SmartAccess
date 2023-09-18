package com.test

import com.IceCreamQAQ.SmartAccess.access.interpreter.MethodInterpreter

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