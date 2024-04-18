dependencies { forge(catalog.forge) }

tasks { remapJar { inputFile.set(project(":common:ingame").tasks.jar.get().archiveFile) } }
