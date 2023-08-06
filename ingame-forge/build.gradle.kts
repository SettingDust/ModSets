dependencies {
    forge(libs.forge)
}

tasks {
    remapJar {
        inputFile.set(project(":ingame").tasks.jar.get().archiveFile)
    }
}
