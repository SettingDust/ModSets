@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    idea

    alias(libs.plugins.dotenv)

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false

    alias(libs.plugins.architectury)
    alias(libs.plugins.architectury.loom) apply false
//    alias(libs.plugins.quilt.loom) apply false
//    alias(libs.plugins.fabric.loom) apply false

    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur) apply false
    alias(libs.plugins.cursegradle)
}

val mod_version: String by project
val maven_group: String by project
val archives_name: String by project
val mod_name: String by rootProject

project.version = "$mod_version+${libs.versions.minecraft.get()}"
project.group = maven_group

architectury {
    minecraft = libs.versions.minecraft.get()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    version = rootProject.version

    base {
        archivesName.set("$archives_name-${project.name}")
    }

    configure<LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
    }

    architectury {
        compileOnly()
    }

    repositories {
        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }
    }

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(
            loom.layered {
                officialMojangMappings()
                parchment(
                    variantOf(rootProject.libs.parchment) {
                        artifactType("zip")
                    },
                )
            },
        )

        implementation(rootProject.libs.kotlin.stdlib.jdk8)
    }

    tasks {
        val properties = mapOf(
            "id" to archives_name,
            "version" to rootProject.version,
            "group" to rootProject.group,
            "name" to mod_name,
            "description" to rootProject.property("mod_description").toString(),
            "author" to rootProject.property("mod_author").toString(),
            "source" to rootProject.property("mod_source").toString(),
            "minecraft_version" to rootProject.libs.versions.min.minecraft.get(),
            "fabric_loader_version" to rootProject.libs.versions.fabric.loader.get(),
            "quilt_loader_version" to rootProject.libs.versions.quilt.loader.get(),
            "forge_version" to rootProject.libs.versions.min.forge.get(),
            "fabric_language_kotlin_version" to rootProject.libs.versions.fabric.language.kotlin.get(),
            "kotlin_forge_version" to rootProject.libs.versions.kotlin.forge.get(),
            "yacl_version" to rootProject.libs.versions.min.yacl.get(),
            "kinecraft_serialization_version" to rootProject.libs.versions.kinecraft.serialization.get(),
            "preloading_tricks_version" to rootProject.libs.versions.preloading.tricks.get(),
            "mod_menu_version" to rootProject.libs.versions.min.modmenu.get(),
            "schema" to "\$schema",
        )

        withType<ProcessResources> {
            inputs.properties(properties)
            filesMatching(listOf("fabric.mod.json", "quilt.mod.json", "META-INF/mods.toml", "*.mixins.json")) {
                expand(properties)
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17

            withSourcesJar()
        }

        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }

        jar {
            from("LICENSE") {
                rename { "${it}_${base.archivesName}" }
            }
        }
    }
}

tasks {
    jar {
        enabled = false
    }
    shadowJar {
        val fabricJar = project(":fabric").tasks.named("remapJar")
        val quiltJar = project(":quilt").tasks.named("remapJar")

        from(fabricJar, quiltJar)

        mergeServiceFiles()


        archiveBaseName.set("$archives_name-fabric-intermediary")
        archiveClassifier.set("")
    }

    build {
        val forgeJar = project(":forge").tasks.named<RemapJarTask>("remapJar")
        dependsOn(shadowJar, forgeJar)
    }
}

curseforge {
    options {
        debug = true
    }
    apiKey = env.CURSEFORGE_TOKEN.value // This should really be in a gradle.properties file
    project {
        id = "890349"
        mainArtifact(tasks.shadowJar.get()) {
            releaseType = "release"
            addGameVersion("fabric")
            addGameVersion("quilt")
            addGameVersion("1.19.4")
            addGameVersion("1.20")
            addGameVersion("1.20.1")
            relations {
                requiredDependency("yacl")
                requiredDependency("fabric-language-kotlin")
                optionalDependency("modmenu")
            }
        }
    }
}
