plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)
    alias(catalog.plugins.fabric.loom)
}

version = rootProject.version

dependencies {
    minecraft(catalog.minecraft)
    mappings(loom.officialMojangMappings())

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
