plugins {
    kotlin("jvm")
}

dependencies{
    api(project(":hibernate"))
    api("org.hibernate:hibernate-entitymanager:5.6.15.Final")
}