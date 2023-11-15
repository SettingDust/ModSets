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

base {
    archivesName.set("$archives_name-fabric")
}

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
                libs.kotlinx.serialization.core.get(),
                libs.kotlinx.serialization.json.get(),
                libs.kotlinx.coroutines.get(),
                libs.kotlin.reflect.get(),
            )
        }
    }
}

tasks {
    processResources {
        from(project(":ingame").sourceSets.main.get().resources)
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

    modImplementation(libs.fabric.loader)
    modRuntimeOnly(libs.fabric.languageKotlin) {
        exclude(module = "fabric-loader")
    }

    modRuntimeOnly(libs.yacl.fabric)
    modRuntimeOnly(libs.modmenu) {
        exclude(module = "fabric-loader")
    }

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-fabric"
    modRuntimeOnly(kinecraft)
    include(kinecraft)

    implementation(libs.preloading.tricks)
    include(libs.preloading.tricks)
}
