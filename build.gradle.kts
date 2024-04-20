@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    idea

    alias(catalog.plugins.kotlin.jvm) apply false
    alias(catalog.plugins.kotlin.plugin.serialization) apply false

    alias(catalog.plugins.architectury)
    alias(catalog.plugins.architectury.loom) apply false
    //    alias(catalog.plugins.quilt.loom) apply false
    //    alias(catalog.plugins.fabric.loom) apply false

    alias(catalog.plugins.shadow)
    alias(catalog.plugins.semver)
}

val mod_version: String by project
val maven_group: String by project
val archives_name: String by project
val mod_name: String by rootProject

project.version = "${semver.semVersion}"

project.group = maven_group

architectury { minecraft = catalog.versions.minecraft.get() }

base { archivesName.set(archives_name) }

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    group = maven_group
    version = rootProject.version

    base { archivesName.set("$archives_name${project.path.replace(":", "-")}") }

    configure<LoomGradleExtensionAPI> { silentMojangMappingsLicense() }

    architectury { compileOnly() }

    repositories { maven("https://maven.parchmentmc.org") { name = "ParchmentMC" } }

    dependencies {
        "minecraft"(rootProject.catalog.minecraft)
        "mappings"(
            loom.layered {
                officialMojangMappings()
                parchment(
                    variantOf(rootProject.catalog.parchment) { artifactType("zip") },
                )
            },
        )

        implementation(rootProject.catalog.kotlin.jdk8)
    }

    tasks {
        val properties =
            mapOf(
                "id" to archives_name,
                "version" to rootProject.version,
                "group" to rootProject.group,
                "name" to mod_name,
                "description" to rootProject.property("mod_description").toString(),
                "author" to rootProject.property("mod_author").toString(),
                "source" to rootProject.property("mod_source").toString(),
                "fabric_loader_version" to rootProject.catalog.versions.fabric.loader.get(),
                "quilt_loader_version" to rootProject.catalog.versions.quilt.loader.get(),
                "forge_version" to rootProject.catalog.versions.min.forge.get(),
                "fabric_language_kotlin_version" to rootProject.catalog.fabric.kotlin.get().version,
                "kotlin_forge_version" to rootProject.catalog.forge.kotlin.get().version,
                "yacl_version" to rootProject.catalog.versions.min.yacl.get(),
                "kinecraft_serialization_version" to
                    rootProject.catalog.kinecraft.serialization.get().version,
                "preloading_tricks_version" to rootProject.catalog.preloading.tricks.get().version,
                "mod_menu_version" to rootProject.catalog.versions.min.modmenu.get(),
                "schema" to "\$schema",
            )

        withType<ProcessResources> {
            inputs.properties(properties)
            filesMatching(
                listOf("fabric.mod.json", "quilt.mod.json", "META-INF/mods.toml", "*.mixins.json")
            ) {
                expand(properties)
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17

            withSourcesJar()
        }

        withType<KotlinCompile> { kotlinOptions { jvmTarget = "17" } }

        jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    }
}

dependencies {
    shadow(project(":fabric")) { isTransitive = false }
    shadow(project(":quilt")) { isTransitive = false }
    shadow(project(":forge")) { isTransitive = false }
}

tasks {
    jar { enabled = false }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        mergeServiceFiles()
        archiveClassifier.set("")

        doFirst {
            manifest {
                from(
                    configurations
                        .flatMap { it.files }
                        .map { zipTree(it) }
                        .map { zip -> zip.find { it.name.equals("MANIFEST.MF") } }
                )
            }
        }
    }

    build { dependsOn(shadowJar) }
}
