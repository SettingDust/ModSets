import net.fabricmc.loom.task.AbstractRunTask

val archives_name: String by rootProject
val mod_name: String by rootProject

plugins { alias(catalog.plugins.shadow) }

loom {
    mods {
        named("main") {
            modFiles.setFrom(tasks.shadowJar.get().archiveFile)
            modSourceSets.empty()
        }
    }
}

architectury {
    platformSetupLoomIde()
    forge()
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }

    // Add KFF Maven repository
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases")
    mavenLocal()
}

dependencies {
    forge(catalog.forge)

    include(project(":common"))
    include(project(":forge:ingame"))

    include(project(":forge:mod"))
    include(project(":forge:setup-mod-hook"))

    shadow(project(":forge:mod-locator")) {
        isTransitive = false
    }

    runtimeOnly(catalog.forge.kotlin)
    modRuntimeOnly(catalog.yacl.forge) { isTransitive = false }

    val kinecraft =
        "maven.modrinth:kinecraft-serialization:${catalog.kinecraft.serialization.get().version}-forge"
    include(kinecraft)

    include(catalog.preloading.tricks)
}

tasks {
    jar { enabled = false }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        destinationDirectory = project.layout.buildDirectory.dir("devlibs")
        archiveClassifier = "dev"

        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
            )
        }
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile = shadowJar.get().archiveFile
    }

    afterEvaluate { withType<AbstractRunTask> { dependsOn(shadowJar) } }
}
