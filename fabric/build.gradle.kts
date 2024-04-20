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

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mixin { add("main", "$archives_name.fabric.refmap.json") }

    mods {
        create(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":common:config"))
            sourceSet("main", project(":common:ingame"))
            dependency(
                catalog.kotlinx.serialization.core.get(),
                catalog.kotlinx.serialization.json.get(),
                catalog.kotlinx.coroutines.get(),
                catalog.kotlin.reflect.get(),
            )
        }
    }
}

tasks { processResources { from(project(":common:ingame").sourceSets.main.get().resources) } }

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

    implementation(project(path = ":common:config", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":common:config"))

    implementation(project(path = ":common:ingame", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(":common:ingame"))

    modImplementation(catalog.fabric.loader)
    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modImplementation(catalog.modmenu) { exclude(module = "fabric-loader") }

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
