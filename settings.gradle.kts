extra["minecraft"] = "1.20.1"

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/common.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/kotlin.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/fabric.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/forge.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/parchmentmc.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/quilt.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/modmenu.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/mixin.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/yacl.gradle.kts")
apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/vanillagradle.gradle.kts")

val minecraft = settings.extra["minecraft"]
val kotlin = settings.extra["kotlin"]

dependencyResolutionManagement.versionCatalogs.named("catalog") {
    library("minecraft-fabric-1.21", "com.mojang", "minecraft").version("1.21")

    plugin("neoforge-moddev", "net.neoforged.moddev").version("1.+")

    // https://linkie.shedaniel.dev/dependencies?loader=neoforge
    library("neoforge", "net.neoforged", "neoforge").version("21.1.54")

    version("min-yacl", "3.0.3")
    version("min-modmenu", "3.0.0")
    version("min-forge", "45")
    version("fabric-loader", "0.16.2")
    version("quilt-loader", "0.24.0")

    library("quilt-standard-libraries-core", "org.quiltmc.qsl", "core").version("6.1.2+$minecraft")

    // https://modrinth.com/mod/preloading-tricks/versions
    library("preloading-tricks", "maven.modrinth", "preloading-tricks").version("1.2.1")
}

val mod_name: String by settings

rootProject.name = mod_name

include("common:config")

include("common:ingame")

include("fabric")

//include("quilt")
//
//include("forge")
//
//include("forge:mod-locator")
//
//include("forge:setup-mod-hook")
//
//include("forge:ingame")
//
//include("forge:mod")
