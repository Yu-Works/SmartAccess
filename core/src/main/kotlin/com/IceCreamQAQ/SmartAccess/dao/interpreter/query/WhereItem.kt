package com.IceCreamQAQ.SmartAccess.dao.interpreter.query

data class WhereItem(
    val key: String,
    val operator: String,
    val needValue: Boolean,
    val infix: String? = null
)