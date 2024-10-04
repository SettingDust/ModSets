plugins {
    alias(catalog.plugins.forge.gradle)
}

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

dependencies {
    minecraft(catalog.minecraft.forge)
    implementation(project(":common"))
}

tasks {
    jar {
        val ingameJarTask = project(":common:ingame").tasks.jar
        from(ingameJarTask.flatMap { it.archiveFile }.map { zipTree(it) })

        manifest {
            attributes(
                "FMLModType" to "GAMELIBRARY"
            )
        }
    }
}
