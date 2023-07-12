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

    alias(libs.plugins.fabric.loom)

    alias(libs.plugins.shadow)
}

val archives_name: String by rootProject
val loom: LoomGradleExtensionAPI by extensions

version = rootProject.version

base {
    archivesName = "$archives_name-common"
}

repositories {
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

    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.hocon)
    api(libs.kotlin.reflect)

    modApi(libs.yacl)
    modApi(libs.modmenu)

    modApi(libs.fabric.loader)
}

tasks {
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
