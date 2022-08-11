import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

group = "cf.wayzer.MindustryContents"
version = System.getenv().getOrDefault("VERSION", "2.0-SNAPSHOT")

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.github.Anuken.Mindustry:core:v137")
    implementation("com.github.CraftTweaker:ZenScript:dde0ab3b5e")
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

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)
    filter(
        filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
        properties = mapOf("tokens" to mapOf("version" to rootProject.version))
    )
}

val shadowTask: ShadowJar = tasks.withType(ShadowJar::class.java) {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    minimize()
}.first()

val jarAndroid = tasks.create("jarAndroid") {
    dependsOn(shadowTask)
    val inFile = shadowTask.archiveFile.get().asFile
    val outFile = inFile.resolveSibling("${shadowTask.archiveBaseName.get()}-Android.jar")
    outputs.file(outFile)
    doLast {
        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot == null || !File(sdkRoot).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        val d8Tool = File("$sdkRoot/build-tools/").listFiles()?.sortedDescending()
            ?.flatMap { dir -> (dir.listFiles().orEmpty()).filter { it.name.startsWith("d8") } }?.firstOrNull()
            ?: throw GradleException("No d8 found. Ensure that you have an Android platform installed.")
        val platformRoot = File("$sdkRoot/platforms/").listFiles()?.sortedDescending()?.firstOrNull { it.resolve("android.jar").exists() }
            ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        //collect dependencies needed for desugaring
        val dependencies = (configurations.compileClasspath.get() + configurations.runtimeClasspath.get() + platformRoot.resolve("android.jar"))
            .joinToString(" ") { "--classpath ${it.path}" }
        exec {
            commandLine("$d8Tool $dependencies --min-api 14 --output $outFile $inFile".split(" "))
            workingDir(inFile.parentFile)
            standardOutput = System.out
            errorOutput = System.err
        }.assertNormalExitValue()
    }
}

tasks.create("install", Copy::class.java) {
    dependsOn(shadowTask)
    from(shadowTask.archiveFile.get())
    into(System.getenv("AppData") + "/mindustry/mods")
}

tasks.create("dist", Jar::class.java) {
    dependsOn(shadowTask)
    dependsOn(jarAndroid)
    from(zipTree(shadowTask.archiveFile.get()))
    from(zipTree(jarAndroid.outputs.files.first()))
    destinationDirectory.set(buildDir.resolve("dist"))
    archiveFileName.set("ContentsTweaker-${rootProject.version}.jar")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.7"
}