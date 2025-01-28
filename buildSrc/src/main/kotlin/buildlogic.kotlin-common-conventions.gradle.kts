plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/GalMichaeli/software-design-buy-product")
        credentials {
            username = project.findProperty("gpr.user") as String?
            password = project.findProperty("gpr.key") as String?
        }
    }
}

val junitVersion = "5.11.3"
val kotestVersion = "5.9.1"
val mockkVersion = "1.13.13"
val kotlinguiceVersion = "3.0.0"

dependencies {
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    // Mocking
    testImplementation("io.mockk:mockk:$mockkVersion")

    // Dependency injection
    implementation("dev.misfitlabs.kotlinguice4:kotlin-guice:$kotlinguiceVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") // JSON
    implementation("com.gitlab.mvysny.konsume-xml:konsume-xml:1.2") // XML

    // External code
    implementation("il.ac.technion.cs.sd:buy-external:1.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}