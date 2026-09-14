plugins {
    kotlin("jvm") version "2.0.21"
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
