plugins {
    kotlin("jvm") version "1.9.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

application {
    mainClass.set("DatabaseSeederKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}