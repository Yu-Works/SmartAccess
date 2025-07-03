plugins {
    kotlin("jvm")
}

dependencies{
    api(project(":hibernate"))
    api("org.hibernate.orm:hibernate-core:6.5.2.Final")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("com.IceCreamQAQ.Rain:test-base:1.0.0-DEV1")
    testImplementation("com.IceCreamQAQ.Rain:event:1.0.0-DEV1")
}

tasks{
    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
kotlin {
    jvmToolchain(11)
}