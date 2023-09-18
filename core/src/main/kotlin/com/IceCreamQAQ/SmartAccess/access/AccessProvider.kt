package com.IceCreamQAQ.SmartAccess.access

interface AccessProvider {

    operator fun <A,B> invoke(accessClass: Class<out Access<A, B>>): Access<A, B>

}