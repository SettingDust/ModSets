import net.fabricmc.loom.task.AbstractRunTask

val archives_name: String by rootProject
val mod_name: String by rootProject

plugins {
    alias(libs.plugins.minotaur)
    alias(libs.plugins.shadow)
}

loom {
    mods {
        named("main") {
            modSourceSets.empty()
            // ClasspathLocator will scan the mod locator that is loaded as SERVICE
            modFiles.setFrom(tasks.remapJar)
        }
    }
}

architectury {
    platformSetupLoomIde()
    forge()
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

    // Add KFF Maven repository
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases")
    mavenLocal()
}

dependencies {
    forge(libs.forge)

    include(project(":config"))
    include(project(":ingame-forge"))

    include(project(":forge-ingame"))
    include(project(":forge-setup-mod-hook"))

    modRuntimeOnly(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge)

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)

    include(libs.preloading.tricks)
}

tasks {
    remapJar {
        val modLocatorRemapJar = project(":forge-mod-locator").tasks.remapJar
        dependsOn(modLocatorRemapJar)
        inputFile.set(modLocatorRemapJar.get().outputs.files.singleFile)
        archiveClassifier.set("")
    }

    afterEvaluate {
        withType<AbstractRunTask> {
            dependsOn(remapJar)
        }
    }
}

modrinth {
    token.set(env.MODRINTH_TOKEN.value) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("mod-sets") // This can be the project ID or the slug. Either will work!
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(tasks.remapJar) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    versionNumber.set("$version-forge")
    gameVersions.addAll(
        "1.19.4",
        "1.20",
        "1.20.1",
    ) // Must be an array, even with only one version
    loaders.add("forge") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    dependencies {
        required.project("kotlin-for-forge")
        // https://modrinth.com/mod/yacl
        required.project("yacl")
        // https://modrinth.com/mod/kinecraft-serialization
        embedded.version("kinecraft-serialization", "${libs.versions.kinecraft.serialization.get()}-forge")
    }
}
