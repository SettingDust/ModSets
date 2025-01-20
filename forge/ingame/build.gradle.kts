plugins {
    alias(catalog.plugins.forge.gradle)
}

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

dependencies {
    minecraft(catalog.lexforge)
    implementation(project(":common"))
}

tasks {
    jar {
        from(project(":common:ingame").tasks.jar.flatMap { it.archiveFile }.map { zipTree(it) })

        manifest {
            attributes(
                "FMLModType" to "GAMELIBRARY"
            )
        }
    }
}
