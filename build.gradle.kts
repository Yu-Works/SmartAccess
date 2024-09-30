plugins {
    java
    kotlin("jvm") version "2.0.10"
}

group = "com.IceCreamQAQ.SmartAccess"
version = "1.0.0-DEV2"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.icecreamqaq.com/repository/maven-public/")
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
        plugin("maven-publish")
        plugin("org.jetbrains.kotlin.jvm")
    }

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        jvmToolchain(8)
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

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>(name) {
                groupId = "com.IceCreamQAQ.SmartAccess"
                artifactId = name
                version = rootProject.version.toString()

                pom {
                    name.set("SmartAccess Java ORM Framework")
                    description.set("SmartAccess Java ORM Framework")
                    url.set("https://github.com/Yu-Works/SmartAccess")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("IceCream")
                            name.set("IceCream")
                            email.set("www@withdata.net")
                        }
                    }
                    scm {
                        connection.set("")
                    }
                }
                from(components["java"])
            }

            repositories {
                mavenLocal()
                maven {
                    val snapshotsRepoUrl = "https://maven.icecreamqaq.com/repository/maven-snapshots/"
                    val releasesRepoUrl = "https://maven.icecreamqaq.com/repository/maven-releases/"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)


                    credentials {
                        System.getenv("MAVEN_USER")?.let { username = it }
                        System.getenv("MAVEN_TOKEN")?.let { password = it }
                    }
                }
            }
        }
    }
}