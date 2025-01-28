plugins {
    id("buildlogic.kotlin-application-conventions")
    kotlin("plugin.serialization") version "2.1.0"
}

dependencies {
    api(project(":library"))
}