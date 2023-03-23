plugins {
    kotlin("jvm") version "1.8.0"
}

group = "com.IceCreamQAQ.SmartAccess"
version = "0.1.0"


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.icecreamqaq.com/repository/maven-public/")
    }
}