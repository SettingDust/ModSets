@file:Suppress("DSL_SCOPE_VIOLATION", "MISSING_DEPENDENCY_CLASS", "FUNCTION_CALL_EXPECTED", "PropertyName")

plugins {
    java
    `maven-publish`
    idea

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false

//    alias(libs.plugins.quilt.loom) apply false
    alias(libs.plugins.fabric.loom) apply false

    alias(libs.plugins.shadow) apply false
}

val mod_version: String by project
val maven_group: String by project
val archives_name: String by project

project.version = "$mod_version+${libs.versions.minecraft.get()}"
project.group = maven_group

val finalJar = tasks.registering(Jar::class) {
    from(project(":fabric").task("remapJar"))
    from(project(":quilt").task("remapJar"))

    archiveBaseName = archives_name
    archiveVersion = "${project.version}"
}
