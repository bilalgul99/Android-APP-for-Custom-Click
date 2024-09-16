buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.1")
    }
}

plugins {
    id("com.android.application") version "8.5.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
