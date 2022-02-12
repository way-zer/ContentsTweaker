plugins {
    kotlin("jvm")
}

subprojects {
    apply { plugin("kotlin") }
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
}