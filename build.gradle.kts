plugins {
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "cf.wayzer.MindustryContents"
version = System.getenv().getOrDefault("VERSION", "1.0-SNAPSHOT")

subprojects {
    apply { plugin("kotlin") }
    apply { plugin("maven-publish") }
    repositories {
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
    tasks.withType(JavaCompile::class) {
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = JavaVersion.VERSION_16.toString()
        options.compilerArgs.addAll(listOf("--release", "8"))
    }
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()

                from(components["kotlin"])
            }
        }
    }
}