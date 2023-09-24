plugins {
    kotlin("jvm")
}

dependencies{
    api(project(":hibernate"))
    api("org.hibernate:hibernate-core-jakarta:5.6.15.Final")
    testImplementation("com.h2database:h2:2.2.224")
}