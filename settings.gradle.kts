apply(
    from = "https://github.com/SettingDust/FabricKotlinTemplate/raw/main/common.settings.gradle.kts"
)

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven2.bai.lol")
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
        maven("https://maven.minecraftforge.net/") { name = "Forge" }
        maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
    }
}

val minecraft = settings.extra["minecraft"]
val kotlin = settings.extra["kotlin"]

dependencyResolutionManagement.versionCatalogs.named("catalog") {
    version("min-yacl", "3.0.3")
    version("min-modmenu", "3.0.0")
    version("min-forge", "45")
    version("fabric-loader", "0.14")
    version("quilt-loader", "0.24.0")

    library("kotlin-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").version("$kotlin")
    val kotlinxSerialization = "1.6.3"
    library("kotlinx-serialization-core", "org.jetbrains.kotlinx", "kotlinx-serialization-core")
        .version(kotlinxSerialization)
    library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
        .version(kotlinxSerialization)
    library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        .version("1.8.0")
    library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").version("$kotlin")

    library("forge-kotlin", "thedarkcolour", "kotlinforforge").version("4.7.0")

    library("quilt-standard-libraries-core", "org.quiltmc.qsl", "core").version("6.1.2+$minecraft")

    plugin("architectury", "architectury-plugin").version("3.+")
    plugin("architectury-loom", "dev.architectury.loom").version("1.6.+")
    plugin("shadow", "com.github.johnrengelman.shadow").version("8.+")

    // https://modrinth.com/mod/preloading-tricks/versions
    library("preloading-tricks", "maven.modrinth", "preloading-tricks").version("1.0.6")
    library("kinecraft-serialization", "maven.modrinth", "kinecraft-serialization")
        .version("1.3.0-fabric")

    library("parchment", "org.parchmentmc.data", "parchment-1.20.1").version("2023.09.03")

    val yacl = "3.2.2+1.20"
    library("yacl-common", "dev.isxander.yacl", "yet-another-config-lib-common").version(yacl)
    library("yacl-fabric", "dev.isxander.yacl", "yet-another-config-lib-fabric").version(yacl)
    library("yacl-forge", "dev.isxander.yacl", "yet-another-config-lib-forge").version(yacl)
}

val mod_name: String by settings

rootProject.name = mod_name

include("common:config")

include("common:ingame")

include("fabric")

include("quilt")

include("forge")

include("forge:mod-locator")

include("forge:setup-mod-hook")

include("forge:ingame")

include("forge:mod")
