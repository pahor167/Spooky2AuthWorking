plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure-Kotlin JVM module. NO Android dependencies on purpose:
// the ported auth/protocol/scan logic must be unit-testable on the JVM
// without an emulator, against the recorded generator dumps.

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
