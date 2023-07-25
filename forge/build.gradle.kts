plugins {
    java
    `maven-publish`

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

val archives_name: String by rootProject
val mod_name: String by rootProject

loom {
    forge {
//        mixinConfig("$archives_name-common.mixins.json")
//        mixinConfig("$archives_name.mixins.json")
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
    maven {
        name = "Sponge Snapshots"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }

    // Add KFF Maven repository
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
}

dependencies {
    forge(libs.forge)

    include(project(path = ":common", configuration = "namedElements"))
    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge.get())

    annotationProcessor(variantOf(libs.mixin) { classifier("processor") })

    include(libs.kinecraft.serialization)
    modRuntimeOnly(libs.kinecraft.serialization)
}

tasks {
    processResources {
        from(project(":common").sourceSets.main.get().resources)
    }
}
