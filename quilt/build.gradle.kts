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

//loom {
//    runs {
//        named("client") {
//            vmArg("-Dloader.workaround.disable_strict_parsing=true")
//            var ""
//            val paths = arrayOf("resources/main", "classes/kotlin/main")
//            for (sub: String in paths) {
//                path + rootProject.projectDir + "/common/build/" + sub + File.pathSeparator
//                path + rootProject.projectDir + "/quilt/build/" + sub + File.pathSeparator
//            }
//            path.substring(0, path.length - 1)
//            vmArg("-Dloader.classPathGroups=$path")
//        }
//    }
//
//    mods {
//        register(archives_name) {
//            modFiles.from("../common/build/devlibs/${project(":common").base.archivesName.get()}-$version-dev.jar")
//            sourceSet(sourceSets.main.get())
//            sourceSet(project(":common").sourceSets.main.get())
//            modFiles.from("../common/build/classes/kotlin/main", "../common/build/resources/main")
//        }
//    }
//}

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
    mavenLocal()
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlin.reflect)

    implementation(project(":config")) {
        isTransitive = false
    }
    include(project(":config"))

    implementation(project(":ingame")) {
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

    implementation(libs.preloading.tricks.local)
    include(libs.preloading.tricks.local)
}

tasks {
    processResources {
        from(project(":ingame").sourceSets.main.get().resources)
    }
}
