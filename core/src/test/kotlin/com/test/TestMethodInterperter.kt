package com.test

import smartaccess.access.interpreter.MethodInterpreter

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