plugins {
    kotlin("jvm")
    `maven-publish`
}

subprojects {
    apply { plugin("kotlin") }
    apply { plugin("maven-publish") }
    sourceSets {
        main {
            java.srcDir("src")
        }
    }
    repositories {
        mavenCentral()
        maven(url = "https://www.jitpack.io")
    }
    dependencies {
        compileOnly("com.github.Anuken.Mindustry:core:v135")
    }

    tasks.getByName<Jar>("jar") {
        archiveClassifier.set(project.name)
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = rootProject.group.toString()
                artifactId = rootProject.name
                version = rootProject.version.toString()

                from(components["kotlin"])
                artifact(tasks.getByName("kotlinSourcesJar"))
            }
        }
    }
}