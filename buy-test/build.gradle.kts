plugins {
    id("buildlogic.kotlin-common-conventions")
}

dependencies {
    testImplementation(project(":buy-app"))
    testImplementation(project(":library"))
    testImplementation("il.ac.technion.cs.sd:buy-external:1.0")
}