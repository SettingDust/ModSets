@file:Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "FUNCTION_CALL_EXPECTED",
    "PropertyName",
    "UnstableApiUsage",
)

import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    idea

    alias(catalog.plugins.kotlin.jvm) apply false
    alias(catalog.plugins.kotlin.plugin.serialization) apply false

    alias(catalog.plugins.neoforge.moddev) apply false

    alias(catalog.plugins.shadow)
    alias(catalog.plugins.git.version)
}

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/gradle_issue_15754.gradle.kts")

val archives_name: String by project
val mod_id: String by rootProject
val mod_name: String by rootProject

group = "${project.property("group")}"

val gitVersion: Closure<String> by extra
version = gitVersion()

base { archivesName.set(archives_name) }

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }

        // Still required by IDEs such as Eclipse and Visual Studio Code
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build"
        // task if it is present.
        // If you remove this line, sources will not be generated.
        withSourcesJar()

        // If this mod is going to be a library, then it should also generate Javadocs in order to
        // aid with development.
        // Uncomment this line to generate them.
        withJavadocJar()
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    base { archivesName.set("${rootProject.base.archivesName.get()}${project.path.replace(":", "-")}") }

    tasks {
        val properties =
            mapOf(
                "id" to archives_name,
                "version" to rootProject.version,
                "group" to rootProject.group,
                "name" to mod_name,
                "description" to rootProject.property("mod_description").toString(),
                "author" to rootProject.property("mod_author").toString(),
                "source" to rootProject.property("mod_source").toString(),
                "fabric_loader_version" to rootProject.catalog.versions.fabric.loader.get(),
                "quilt_loader_version" to rootProject.catalog.versions.quilt.loader.get(),
                "forge_version" to rootProject.catalog.versions.min.forge.get(),
                "fabric_language_kotlin_version" to rootProject.catalog.fabric.kotlin.get().version,
                "kotlin_forge_version" to rootProject.catalog.kotlin.forge.asProvider().get().version,
                "yacl_version" to rootProject.catalog.versions.min.yacl.get(),
                "kinecraft_serialization_version" to
                    rootProject.catalog.kinecraft.serialization.get().version,
                "preloading_tricks_version" to rootProject.catalog.preloading.tricks.get().version,
                "mod_menu_version" to rootProject.catalog.versions.min.modmenu.get(),
                "schema" to "\$schema",
            )

        withType<ProcessResources> {
            inputs.properties(properties)
            filesMatching(
                listOf(
                    "fabric.mod.json",
                    "quilt.mod.json",
                    "META-INF/mods.toml",
                    "META-INF/neoforge.mods.toml",
                    "*.mixins.json"
                )
            ) {
                expand(properties)
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17

            withSourcesJar()
        }

        jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    }
}

dependencies {
    shadow(project(":fabric")) { isTransitive = false }
    shadow(project(":quilt")) { isTransitive = false }
    shadow(project(":forge")) { isTransitive = false }
}

tasks {
    jar { enabled = false }

    shadowJar {
        dependsOn(":forge:shadowJar")
        configurations = listOf(project.configurations.shadow.get())
        mergeServiceFiles()
        archiveClassifier.set("")

        doFirst {
            manifest {
                from(
                    configurations
                        .flatMap { it.files }
                        .map { zipTree(it) }
                        .map { zip -> zip.find { it.name.equals("MANIFEST.MF") } }
                )
            }
        }
    }

    build { dependsOn(shadowJar) }
}
