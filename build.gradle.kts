// Top-level build file. Module config in :app/build.gradle.kts
buildscript {
    dependencies {
        // Pin javapoet to 1.13.0+ to avoid Hilt plugin classpath shadowing.
        classpath("com.squareup:javapoet:1.13.0")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
