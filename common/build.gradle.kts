@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

val archives_name: String by rootProject

architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
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

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
}

dependencies {
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines)
    api(libs.kotlin.reflect)

    modApi(libs.yacl.common)
    modApi(libs.modmenu)

    modApi("maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-fabric")
}

tasks {
    jar {
        manifest.attributes(
            "FMLModType" to "LIBRARY",
        )
    }
}
