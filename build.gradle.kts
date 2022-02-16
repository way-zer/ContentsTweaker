plugins {
    kotlin("jvm") version "1.6.10"
}

group = "cf.wayzer"
version = System.getenv().getOrDefault("VERSION", "1.0-SNAPSHOT")