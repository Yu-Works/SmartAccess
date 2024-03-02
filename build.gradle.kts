plugins {
    java
    kotlin("jvm") version "1.9.10"
}

group = "com.IceCreamQAQ.SmartAccess"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.icecreamqaq.com/repository/maven-public/")
}

subprojects {
    apply {
        plugin("java")
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.icecreamqaq.com/repository/maven-public/")
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("com.IceCreamQAQ.Rain:application:1.0.0-DEV1")
    }
}