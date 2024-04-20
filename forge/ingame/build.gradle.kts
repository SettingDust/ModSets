dependencies { forge(catalog.forge) }

tasks {
    remapJar {
        val task = project(":common:ingame").tasks.jar
        dependsOn(task)
        inputFile.set(task.get().archiveFile)
    }
}
