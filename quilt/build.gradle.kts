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
}

val archives_name: String by rootProject
val loom: LoomGradleExtensionAPI by extensions

loom {
    runs {
        named("client") {
            vmArg("-Dloader.workaround.disable_strict_parsing=true")
            var path = ""
            val paths = arrayOf("resources/main", "classes/kotlin/main")
            for (sub: String in paths) {
                path = path + rootProject.projectDir + "/common/build/" + sub + File.pathSeparator
                path = path + rootProject.projectDir + "/quilt/build/" + sub + File.pathSeparator
            }
            path = path.substring(0, path.length - 1)
            vmArg("-Dloader.classPathGroups=$path")
        }
    }

    mods {
        register(archives_name) {
            modFiles.from("../common/build/devlibs/${project(":common").base.archivesName.get()}-$version-dev.jar")
            sourceSet(sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
            modFiles.from("../common/build/classes/kotlin/main", "../common/build/resources/main")
        }
    }
}

repositories {
    maven("https://maven.fabricmc.net/")

    maven {
        name = "Quilt"
        url = uri("https://maven.quiltmc.org/repository/release")
    }

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
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)

    include(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }

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

    modRuntimeOnly(libs.quilted.fabric.api) {
        exclude(module = "quilt-loader")
    }

    modRuntimeOnly(libs.kinecraft.serialization)
    include(libs.kinecraft.serialization)
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
