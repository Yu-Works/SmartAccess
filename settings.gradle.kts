rootProject.name = "SmartAccess"

fun includeProject(name: String, dir: String? = null) {
    include(name)
    dir?.let { project(name).projectDir = file(it) }
}

// core
includeProject(":core", "core")

// jdbc
fun jdbc(name: String) {
    includeProject(":$name", "jdbc/$name")
}

jdbc("jdbc-core")
jdbc("jdbc-jpa")


// reactive
fun reactive(name: String) {
    includeProject(":$name", "reactive/$name")
}

reactive("reactive-core")
reactive("reactive-r2dbc")
reactive("reactive-vertx")


// framework
fun framework(name: String, dir: String = name) {
    includeProject(":$name", "framework/$dir")
}

framework("hibernate", "hibernate/hibernate")
framework("hibernate-reactive", "hibernate/hibernate-reactive")