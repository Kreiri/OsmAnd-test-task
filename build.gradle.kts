// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        // Overrides the built-in AGP 9.0 Kotlin version to a newer one
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}