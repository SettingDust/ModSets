import net.fabricmc.loom.task.AbstractRunTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory

val archives_name: String by rootProject
val mod_name: String by rootProject

plugins {
    alias(libs.plugins.shadow)
}

loom {
    mods {
        named("main") {
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
        filter {
            includeGroup("maven.modrinth")
        }
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
    forge(libs.forge)

    include(project(":config"))
    include(project(":ingame-forge"))

    include(project(":forge-ingame"))
    include(project(":forge-setup-mod-hook"))

    runtimeOnly(project(":forge-mod-locator"))

    runtimeOnly(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge) {
        isTransitive = false
    }

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)

    include(libs.preloading.tricks)
}

tasks {
    remapJar {
        val modLocatorRemapJar = project(":forge-mod-locator").tasks.remapJar
        dependsOn(modLocatorRemapJar)
        inputFile.set(modLocatorRemapJar.get().outputs.files.singleFile)
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.libsDirectory)
    }

    afterEvaluate {
        withType<AbstractRunTask> {
            dependsOn(remapJar)
        }
    }
}
