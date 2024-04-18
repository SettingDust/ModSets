@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

val archives_name: String by rootProject
val mod_name: String by rootProject

version = rootProject.version

base { archivesName.set("$archives_name-fabric") }

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        create(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":config"))
            sourceSet("main", project(":ingame"))
            dependency(
                catalog.kotlinx.serialization.core.get(),
                catalog.kotlinx.serialization.json.get(),
                catalog.kotlinx.coroutines.get(),
                catalog.kotlin.reflect.get(),
            )
        }
    }
}

tasks { processResources { from(project(":ingame").sourceSets.main.get().resources) } }

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

    modImplementation(catalog.fabric.loader)
    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modRuntimeOnly(catalog.modmenu) { exclude(module = "fabric-loader") }

    val kinecraft =
        "maven.modrinth:kinecraft-serialization:${catalog.kinecraft.serialization.get().version}-fabric"
    modRuntimeOnly(kinecraft)
    include(kinecraft)

    implementation(catalog.preloading.tricks)
    include(catalog.preloading.tricks)
}
