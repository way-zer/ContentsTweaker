plugins {
    kotlin("jvm")
}

sourceSets {
    main {
        java.srcDir("src")
    }
}

dependencies {
    api(kotlin("stdlib"))
    compileOnly("com.github.Anuken.Mindustry:core:v135")
}