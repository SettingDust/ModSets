@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.build.nesting.IncludedJarFactory
import net.fabricmc.loom.task.RemapJarTask

val archives_name: String by rootProject
val loom: LoomGradleExtensionAPI by extensions

architectury {
    platformSetupLoomIde()
    loader("quilt")
}

loom {
    mods {
        register(archives_name) {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
        }
    }

    runs {
        named("client") {
            property("mixin.debug.export", "true")
            property("mixin.debug.verbose", "true")
        }
    }
}

//loom {
//    runs {
//        named("client") {
//            vmArg("-Dloader.workaround.disable_strict_parsing=true")
//            var path = ""
//            val paths = arrayOf("resources/main", "classes/kotlin/main")
//            for (sub: String in paths) {
//                path = path + rootProject.projectDir + "/common/build/" + sub + File.pathSeparator
//                path = path + rootProject.projectDir + "/quilt/build/" + sub + File.pathSeparator
//            }
//            path = path.substring(0, path.length - 1)
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
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlin.reflect)

    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(path = ":common", configuration = "transformProductionQuilt"))

    implementation(project.project(":common").sourceSets.named("game").get().output)

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
}

tasks {
    processResources {
        from(project(":common").sourceSets.main.get().resources)
    }
}

evaluationDependsOn(":common")

tasks.remapJar {
    val remapCommonModJar = project(":common").tasks.getByName<RemapJarTask>("remapModJar")
    dependsOn(remapCommonModJar)
    mustRunAfter(remapCommonModJar)
    val factory = IncludedJarFactory(project)
    val getNestableJar = IncludedJarFactory::class.java.getDeclaredMethod(
        "getNestableJar",
        File::class.java,
        IncludedJarFactory.Metadata::class.java
    )
    getNestableJar.isAccessible = true

    // TODO Need a gradle plugin to run the task before
    if (remapCommonModJar.outputs.files.singleFile.exists())
        nestedJars.from(
            getNestableJar.invoke(
                factory,
                project.project(":common").tasks.named<RemapJarTask>("remapModJar").get().outputs.files.singleFile,
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.common",
                    "game",
                    version.toString(),
                    "game"
                )
            )
        )
}
