import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val nettyVersion = "4.1.91.Final"
val kotlinVersion = "1.80.20"

plugins {
    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "io.netty", name = "netty-all", version = nettyVersion)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}