import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "cf.wayzer"
version = System.getenv().getOrDefault("VERSION", "3.0-SNAPSHOT")

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.github.Anuken.Arc:arc-core:v149")
    compileOnly("com.github.anuken.mindustry:core:v149") {
        exclude(group = "com.github.Anuken.Arc")
    }
}

kotlin {
    jvmToolchain(8)
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

val jarAndroid by tasks.registering {
    dependsOn(shadowTask)
    val inFile = shadowTask.archiveFile.get().asFile
    val outFile = inFile.resolveSibling("${shadowTask.archiveBaseName.get()}-Android.jar")
    outputs.file(outFile)
    doLast {
        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot == null || !File(sdkRoot).exists()) throw GradleException("No valid Android SDK found. Ensure that ANDROID_HOME is set to your Android SDK directory.")

        val buildToolsDir = File(sdkRoot, "build-tools")
        val d8Tool = buildToolsDir.listFiles()?.sortedDescending()
            ?.flatMap { dir -> dir.listFiles()?.filter { it.name == "d8" || it.name == "d8.bat" } ?: emptyList() }
            ?.firstOrNull()
            ?: throw GradleException("No d8 found. Ensure that you have an Android build-tools installed (>= 28.0.0).")
        val platformRoot = File(sdkRoot, "platforms").listFiles()
            ?.sortedDescending()
            ?.firstOrNull { File(it, "android.jar").exists() }
            ?: throw GradleException("No android.jar found. Ensure that you have an Android platform installed.")

        val androidJar = File(platformRoot, "android.jar")
        val dependencies = (configurations.getByName("compileClasspath") +
                configurations.getByName("runtimeClasspath") + files(androidJar)).files
        val classpathArgs = dependencies.flatMap { listOf("--classpath", it.absolutePath) }
        outFile.parentFile.mkdirs()

        // 使用 ProviderFactory.exec (推荐)
        project.providers.exec {
            commandLine = listOf(d8Tool.absolutePath) + classpathArgs +
                    listOf("--min-api", "14", "--output", outFile.absolutePath, inFile.absolutePath)
            workingDir(inFile.parentFile)
        }.result.get().assertNormalExitValue()
    }
}

tasks.register("devInstall", Copy::class.java) {
    dependsOn(shadowTask)
    from(shadowTask.archiveFile.get())
    into(System.getenv("AppData") + "/mindustry/mods")
}

tasks.register("dist", Jar::class.java) {
    dependsOn(shadowTask)
    dependsOn(jarAndroid)
    from(zipTree(shadowTask.archiveFile.get()))
    from(zipTree(jarAndroid.map { outputs.files.first() }))
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
    archiveFileName.set("ContentsTweaker-${rootProject.version}.jar")
}