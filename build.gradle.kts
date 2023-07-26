@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    idea

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false

    alias(libs.plugins.architectury.loom) apply false
//    alias(libs.plugins.quilt.loom) apply false
//    alias(libs.plugins.fabric.loom) apply false

    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.minotaur)
    alias(libs.plugins.curseforge)
}

val mod_version: String by project
val maven_group: String by project
val archives_name: String by project
val mod_name: String by rootProject

project.version = "$mod_version+${libs.versions.minecraft.get()}"
project.group = maven_group

val finalJar by tasks.registering(Jar::class) {
    dependsOn(":fabric:remapJar")
    from(zipTree(project(":fabric").tasks.getByName("remapJar").outputs.files.first()))
//    from(project(":quilt").tasks.getByName("remapJar"))

    archiveBaseName.set(archives_name)
    archiveVersion.set("${project.version}")
}

val publishCurseforge by tasks.registering(TaskPublishCurseForge::class) {
    apiToken = System.getenv("CURSEFORGE_TOKEN")
    // https://legacy.curseforge.com/minecraft/mc-mods/mod-sets
    upload(890349, finalJar) {
        addModLoader("fabric")
        addGameVersion("1.19.4", "1.20", "1.20.1")
        addRequirement("yacl", "fabric-language-kotlin")
        addOptional("modmenu")
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("mod-sets") // This can be the project ID or the slug. Either will work!
    syncBodyFrom.set(rootProject.file("README.md").readText())
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(finalJar) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    changelog.set(
        """
        fix: add more readable info for errors
        """.trimIndent(),
    )
    gameVersions.addAll(
        "1.19.4",
        "1.20",
        "1.20.1",
    ) // Must be an array, even with only one version
    loaders.add("fabric") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    dependencies {
        required.project("fabric-language-kotlin")
        // https://modrinth.com/mod/yacl
        required.project("yacl")
        // https://modrinth.com/mod/kinecraft-serialization
        embedded.version("kinecraft-serialization", libs.versions.kinecraft.serialization.get())
        optional.project("modmenu")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "maven-publish")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    version = rootProject.version

    base {
        archivesName.set("$archives_name-${project.name}")
    }

    configure<LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
    }

    repositories {
        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }
    }

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "mappings"(
            loom.layered {
                officialMojangMappings()
                parchment(
                    variantOf(rootProject.libs.parchment) {
                        artifactType("zip")
                    },
                )
            },
        )

        implementation(rootProject.libs.kotlin.stdlib.jdk8)
    }

    tasks {
        val properties = mapOf(
            "id" to archives_name,
            "version" to rootProject.version,
            "group" to rootProject.group,
            "name" to mod_name,
            "description" to rootProject.property("mod_description").toString(),
            "author" to rootProject.property("mod_author").toString(),
            "source" to rootProject.property("mod_source").toString(),
            "minecraft_version" to rootProject.libs.versions.min.minecraft.get(),
            "fabric_loader_version" to rootProject.libs.versions.fabric.loader.get(),
            "quilt_loader_version" to rootProject.libs.versions.quilt.loader.get(),
            "forge_version" to rootProject.libs.versions.min.forge.get(),
            "fabric_language_kotlin_version" to rootProject.libs.versions.fabric.language.kotlin.get(),
            "kotlin_forge_version" to rootProject.libs.versions.kotlin.forge.get(),
            "yacl_version" to rootProject.libs.versions.min.yacl.get(),
            "kinecraft_serialization_version" to rootProject.libs.versions.kinecraft.serialization.get(),
            "mod_menu_version" to rootProject.libs.versions.min.modmenu.get(),
            "schema" to "\$schema",
        )

        withType<ProcessResources> {
            inputs.properties(properties)
            filesMatching(listOf("fabric.mod.json", "quilt.mod.json", "META-INF/mods.toml", "*.mixins.json")) {
                expand(properties)
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17

            withSourcesJar()
        }

        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
            }
        }

        jar {
            from("LICENSE") {
                rename { "${it}_${base.archivesName}" }
            }
        }
    }
}
