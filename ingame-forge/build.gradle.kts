dependencies { forge(catalog.forge) }

tasks { remapJar { inputFile.set(project(":ingame").tasks.jar.get().archiveFile) } }
