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
    alias(libs.plugins.minotaur)
}

val mod_version: String by project
val maven_group: String by project
val archives_name: String by project

project.version = "$mod_version+${libs.versions.minecraft.get()}"
project.group = maven_group

val finalJar by tasks.registering(Jar::class) {
    dependsOn(":fabric:remapJar")
    from(zipTree(project(":fabric").tasks.getByName("remapJar").outputs.files.first()))
//    from(project(":quilt").tasks.getByName("remapJar"))

    archiveBaseName = archives_name
    archiveVersion = "${project.version}"
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("mod-sets") // This can be the project ID or the slug. Either will work!
    syncBodyFrom.set(rootProject.file("README.md").readText())
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(finalJar) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    gameVersions.addAll(
        "1.19.2",
    ) // Must be an array, even with only one version
    loaders.add("fabric") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    dependencies {
        required.version("Ha28R6CL", libs.versions.fabric.language.kotlin.get())
        // https://modrinth.com/mod/yacl
        required.version("yacl", libs.versions.yacl.get())
        // https://modrinth.com/mod/kinecraft-serialization
        embedded.version("epmEbmF0", libs.versions.kinecraft.serialization.get())
    }
}
