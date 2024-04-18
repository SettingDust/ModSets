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
    mods {
        create(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":config"))
            sourceSet("main", project(":ingame"))
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

    implementation(project(path = ":config", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":config"))

    implementation(project(path = ":ingame", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":ingame"))

    modImplementation(catalog.quilt.loader)
    modImplementation(catalog.quilt.standard.libraries.core)

    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modRuntimeOnly(catalog.modmenu) { isTransitive = false }

    modRuntimeOnly(catalog.quilt.fabric.api)

    val kinecraft =
        "maven.modrinth:kinecraft-serialization:${catalog.kinecraft.serialization.get().version}-fabric"
    modRuntimeOnly(kinecraft)
    include(kinecraft)

    implementation(catalog.preloading.tricks)
    include(catalog.preloading.tricks)
}

tasks { processResources { from(project(":ingame").sourceSets.main.get().resources) } }
