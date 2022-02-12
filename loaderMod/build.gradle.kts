plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}
dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.github.Anuken.Mindustry:core:v135")
    findProject(":contents")!!.childProjects.values.forEach {
        implementation(project(it.path))
    }
}

tasks.withType(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    minimize()
}