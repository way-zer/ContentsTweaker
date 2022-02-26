plugins {
    kotlin("jvm")
    `maven-publish`
}

sourceSets {
    main {
        java.srcDir("src")
    }
}

dependencies {
    api(project(":core"))
    compileOnly("com.github.Anuken.Mindustry:core:v135")
    childProjects.values.forEach {
        implementation(project(it.path))
    }
}

subprojects {
    sourceSets {
        main {
            java.srcDir("src")
        }
    }

    dependencies {
        api(project(":core"))
        compileOnly("com.github.Anuken.Mindustry:core:v135")
    }
}