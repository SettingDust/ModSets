@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.api.LoomGradleExtensionAPI

val archives_name: String by rootProject
val loom: LoomGradleExtensionAPI by extensions

architectury {
    platformSetupLoomIde()
    loader("quilt")
}

loom {
    mixin {}

    mods {
        create(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":common"))
            sourceSet("main", project(":common:ingame"))
        }
    }
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

    maven("https://maven.fabricmc.net/")

    maven {
        name = "Quilt"
        url = uri("https://maven.quiltmc.org/repository/release")
    }

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
    mavenLocal()
}

dependencies {
    implementation(catalog.kotlinx.serialization.core)
    implementation(catalog.kotlinx.serialization.json)
    implementation(catalog.kotlinx.coroutines)
    implementation(catalog.kotlin.reflect)

    implementation(project(":common")) {
        isTransitive = false
    }
    include(project(":common"))

    implementation(project(":common:ingame")) {
        isTransitive = false
    }
    include(project(":common:ingame"))

    modImplementation(catalog.quilt.loader)
    modImplementation(catalog.quilt.standard.libraries.core)

    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modImplementation(catalog.modmenu) { isTransitive = false }

    modRuntimeOnly(catalog.quilt.fabric.api)

    catalog.kinecraft.serialization.get().copy().let {
        it.version { require("$requiredVersion-fabric") }
        modRuntimeOnly(it)
        include(it)
    }

    catalog.preloading.tricks.let {
        implementation(it)
        include(it)
    }
}

tasks { processResources { from(project(":common:ingame").sourceSets.main.get().resources) } }
