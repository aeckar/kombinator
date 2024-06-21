plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

group = "com.github.aeckar"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.aeckar:kanary:master-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.gradle.sample"
            artifactId = "kombinator"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}