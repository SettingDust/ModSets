dependencyResolutionManagement {
    pluginManagement {
        repositories {
            maven("https://maven.architectury.dev/")
            maven("https://maven.fabricmc.net/")
            maven("https://maven.minecraftforge.net/")
            maven("https://maven2.bai.lol")
            maven("https://repo.spongepowered.org/repository/maven-public/")
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement.versionCatalogs.create("catalog") {
    // https://github.com/palantir/gradle-git-version
    plugin("git-version", "com.palantir.git-version").version("3.+")

    plugin("shadow", "com.gradleup.shadow").version("8.+")

    plugin("explosion", "lol.bai.explosion").version("0.2.0")

    library("minecraft-fabric-1.21", "com.mojang", "minecraft").version("1.21")


    val minecraft = "1.20.1"
    version("minecraft", minecraft)

    val kotlin = "2.0.20"
    version("kotlin", kotlin)
    plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version(kotlin)
    plugin("kotlin-plugin-serialization", "org.jetbrains.kotlin.plugin.serialization").version(kotlin)

    library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").version(kotlin)

    val kotlinxSerialization = "1.7.3"
    library("kotlinx-serialization-core", "org.jetbrains.kotlinx", "kotlinx-serialization-core").version(
        kotlinxSerialization
    )
    library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").version(
        kotlinxSerialization
    )

    library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.10.1")

    // https://modrinth.com/mod/kinecraft-serialization/versions
    library("kinecraft-serialization", "maven.modrinth", "kinecraft-serialization").version("1.16.1")

    library("minecraft", "com.mojang", "minecraft").version("1.20.1")

    plugin("vanilla-gradle", "org.spongepowered.gradle.vanilla").version("0.2.1-SNAPSHOT")


    plugin("fabric-loom", "fabric-loom").version("1.9.+")
    // https://linkie.shedaniel.dev/dependencies?loader=fabric
    version("fabric-loader", "0.16.9")
    version("fabric-api", "0.92.2+$minecraft")
    library("fabric-loader", "net.fabricmc", "fabric-loader").version("0.16.9")
    library("fabric-api", "net.fabricmc.fabric-api", "fabric-api").version("0.92.2+$minecraft")
    library("fabric-kotlin", "net.fabricmc", "fabric-language-kotlin").version("1.12.2+kotlin.$kotlin")

    plugin("forge-gradle", "net.minecraftforge.gradle").version("6.+")
    // https://linkie.shedaniel.dev/dependencies?loader=forge
    version("lexforge", "47.3.12")
    library("lexforge", "net.minecraftforge", "forge").version("1.20.1-47.3.12")
    library("forgified-fabric-api", "dev.su5ed.sinytra.fabric-api", "fabric-api").version("0.92.2+1.11.8+$minecraft")
    library("sinytra-connector", "org.sinytra", "Connector").version("1.0.0-beta.46+$minecraft")
    library("kotlin-forge", "thedarkcolour", "kotlinforforge").version("4.11.0")

    plugin("neoforge-moddev", "net.neoforged.moddev").version("1.+")

    // https://linkie.shedaniel.dev/dependencies?loader=neoforge
    library("neoforge", "net.neoforged", "neoforge").version("21.1.54")

    version("min-yacl", "3.6.0")
    version("min-modmenu", "3.0.0")
    version("min-forge", "45")
    version("quilt-loader", "0.24.0")

    library("quilt-loader", "org.quiltmc", "quilt-loader").version("0.26.4")
    plugin("quilt-loom", "org.quiltmc.loom").version("1.8.+")
    library("quilt-standard-libraries-core", "org.quiltmc.qsl", "core").version("6.2.0+$minecraft")
    library(
        "quilt-fabric-api",
        "org.quiltmc.quilted-fabric-api",
        "quilted-fabric-api"
    ).version("7.6.0+0.92.2-$minecraft")


    val yaclVersion = "3.6.2"
    library("yacl-fabric", "dev.isxander", "yet-another-config-lib").version("$yaclVersion+1.20.1-fabric")
    library("yacl-forge", "dev.isxander", "yet-another-config-lib").version("$yaclVersion+1.20.1-forge")

    library("modmenu", "com.terraformersmc", "modmenu").version("7.2.2")

    // https://modrinth.com/mod/preloading-tricks/versions
    library("preloading-tricks", "maven.modrinth", "preloading-tricks").version("1.2.3")

    // https://modrinth.com/mod/preloading-tricks/versions
    library("connector", "maven.modrinth", "connector").version("1.0.0-beta.46+1.20.1")
}

val mod_name: String by settings

rootProject.name = mod_name

include("common")
include("common:ingame")

include("fabric")
include("fabric:ingame")

include("quilt")

include("forge:mod-locator")
include("forge:setup-mod-hook")
include("forge:ingame")
include("forge:mod")
