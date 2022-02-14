import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)
    filter(
        filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
        properties = mapOf("tokens" to mapOf("version" to rootProject.version))
    )
}

val shadowTask: ShadowJar = tasks.withType(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    minimize()
}.first()

tasks.create("dist", Copy::class.java) {
    dependsOn(shadowTask)
    from(shadowTask.archiveFile){
        rename { "ContentsLoader-${rootProject.version}.jar" }
    }
    into(buildDir.resolve("dist"))
}