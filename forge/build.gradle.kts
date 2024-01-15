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

    shadow(project(path = ":forge-mod-locator", configuration = "namedElements")) {
        isTransitive = false
    }

    runtimeOnly(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge) {
        isTransitive = false
    }

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)

    include(libs.preloading.tricks)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        configurations = listOf(project.configurations.getByName("shadow"))
        destinationDirectory.set(project.buildDir.resolve("devlibs"))
        archiveClassifier.set("dev")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        destinationDirectory.set(rootProject.libsDirectory)
    }

    afterEvaluate {
        withType<AbstractRunTask> {
            dependsOn(shadowJar)
        }
    }
}
