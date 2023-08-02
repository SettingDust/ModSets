@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.fabricmc.loom.build.nesting.IncludedJarFactory
import net.fabricmc.loom.task.RemapJarTask

plugins {
    alias(libs.plugins.minotaur)
}

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

tasks {
    processResources {
        from(project(":common").sourceSets.main.get().resources)
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
}

dependencies {
    implementation(project(path = ":common", configuration = "namedElements")) {
        exclude(module = "fabric-loader")
    }
    include(project(path = ":common", configuration = "transformProductionFabric"))

    implementation(project.project(":common").sourceSets.named("game").get().output)

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
}

tasks {
    remapJar {
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

        if (remapCommonModJar.outputs.files.singleFile.exists())
            nestedJars.from(
                getNestableJar.invoke(
                    factory,
                    remapCommonModJar.outputs.files.singleFile,
                    IncludedJarFactory.Metadata(
                        "settingdust.modsets.common",
                        "game",
                        version.toString(),
                        "game"
                    )
                )
            )
    }
}


modrinth {
    token.set(env.MODRINTH_TOKEN.value) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("mod-sets") // This can be the project ID or the slug. Either will work!
    syncBodyFrom.set(rootProject.file("README.md").readText())
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(rootProject.tasks.named("fabricIntermediaryJar")) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    versionNumber.set("$version-fabric-intermediary")
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    gameVersions.addAll("1.19.2") // Must be an array, even with only one version
    loaders.add("fabric") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    loaders.add("quilt")
    dependencies {
        required.project("fabric-language-kotlin")
        // https://modrinth.com/mod/yacl
        required.project("yacl")
        // https://modrinth.com/mod/kinecraft-serialization
        embedded.version("kinecraft-serialization", "${libs.versions.kinecraft.serialization.get()}-fabric")
        optional.project("modmenu")
    }
}
