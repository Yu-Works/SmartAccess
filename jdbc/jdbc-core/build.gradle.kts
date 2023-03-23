plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core"))
    api("com.zaxxer:HikariCP:4.0.3")
}