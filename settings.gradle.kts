rootProject.name = "ModSets"

include("config")
include("ingame")
include("ingame-forge")
include("fabric")
include("quilt")
include("forge")
include("forge-mod-locator")
include("forge-setup-mod-hook")
include("forge-ingame")

pluginManagement {
    repositories {
        maven("https://maven.architectury.dev/")
        maven {
            name = "Quilt"
            url = uri("https://maven.quiltmc.org/repository/release")
        }
        // Currently needed for Intermediary and other temporary dependencies
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForge"
            url = uri("https://maven.neoforged.net/releases")
        }

        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Sponge Snapshots"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
        gradlePluginPortal()
        mavenLocal()
    }
}
