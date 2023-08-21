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
        filter {
            includeGroup("maven.modrinth")
        }
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
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlin.reflect)

    implementation(project(path = ":config", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":config"))

    implementation(project(path = ":ingame", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":ingame"))

    modImplementation(libs.quilt.loader)
    modImplementation(libs.quilt.standard.libraries.core)

    modRuntimeOnly(libs.fabric.languageKotlin) {
        exclude(module = "fabric-loader")
    }

    modRuntimeOnly(libs.yacl.fabric) {
        isTransitive = false
    }
    modRuntimeOnly(libs.modmenu) {
        isTransitive = false
    }

    modRuntimeOnly(libs.quilted.fabric.api)

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-fabric"
    modRuntimeOnly(kinecraft)
    include(kinecraft)

    implementation(libs.preloading.tricks)
    include(libs.preloading.tricks)
}

tasks {
    processResources {
        from(project(":ingame").sourceSets.main.get().resources)
    }
}
