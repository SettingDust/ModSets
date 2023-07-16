@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    `maven-publish`

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)

    alias(libs.plugins.quilt.loom)
}

val archives_name: String by rootProject
val loom: LoomGradleExtensionAPI by extensions

version = rootProject.version

base {
    archivesName = "$archives_name-quilt"
}

loom {
    runs {
        named("client") {
            vmArg("-Dloader.workaround.disable_strict_parsing=true")
        }
    }
}

repositories {
    maven("https://maven.fabricmc.net/")

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

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
}

dependencies {
    "minecraft"(libs.minecraft)
    "mappings"(
        loom.layered {
            officialMojangMappings()
            parchment(
                variantOf(libs.parchment) {
                    artifactType("zip")
                },
            )
        },
    )

    include(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":common", configuration = "namedElements"))

    modImplementation(libs.quilt.loader)
    modImplementation(libs.quilt.standard.libraries.core)

    modRuntimeOnly(libs.fabric.languageKotlin) {
        exclude(module = "fabric-loader")
    }

    modRuntimeOnly(libs.yacl) {
        exclude(module = "fabric-loader")
    }
    modRuntimeOnly(libs.modmenu) {
        exclude(module = "fabric-loader")
    }

    modRuntimeOnly(libs.quilted.fabric.api) {
        exclude(module = "quilt-loader")
    }

    modRuntimeOnly(libs.kinecraft.serialization)
    include(libs.kinecraft.serialization)
}

tasks {
    processResources {
        inputs.property("id", archives_name)
        inputs.property("version", rootProject.version)
        inputs.property("group", rootProject.group)
        inputs.property("name", rootProject.property("mod_name").toString())
        inputs.property("description", rootProject.property("mod_description").toString())
        inputs.property("author", rootProject.property("mod_author").toString())
        inputs.property("source", rootProject.property("mod_source").toString())
        inputs.property("minecraft_version", libs.versions.minecraft.get())
        inputs.property("quilt_loader_version", libs.versions.quilt.loader.get())
        inputs.property("fabric_language_kotlin_version", libs.versions.fabric.language.kotlin.get())
        inputs.property("yacl_version", libs.versions.yacl.get())
        inputs.property("kinecraft_serialization_version", libs.versions.kinecraft.serialization.get())
        inputs.property("mod_menu_version", libs.versions.modmenu.get())

        filesMatching("quilt.mod.json") {
            expand(
                "id" to archives_name,
                "version" to rootProject.version,
                "group" to rootProject.group,
                "name" to rootProject.property("mod_name").toString(),
                "description" to rootProject.property("mod_description").toString(),
                "author" to rootProject.property("mod_author").toString(),
                "source" to rootProject.property("mod_source").toString(),
                "minecraft_version" to libs.versions.minecraft.get(),
                "quilt_loader_version" to libs.versions.quilt.loader.get(),
                "fabric_language_kotlin_version" to libs.versions.fabric.language.kotlin.get(),
                "yacl_version" to libs.versions.yacl.get(),
                "kinecraft_serialization_version" to libs.versions.kinecraft.serialization.get(),
                "mod_menu_version" to libs.versions.modmenu.get(),
                "schema" to "\$schema",
            )
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        withSourcesJar()
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${base.archivesName}" }
        }
    }
}
