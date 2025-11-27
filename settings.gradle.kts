dependencyResolutionManagement {
    pluginManagement {
        repositories {
            mavenCentral()
            gradlePluginPortal()
            maven("https://maven.msrandom.net/repository/cloche")
            maven("https://raw.githubusercontent.com/SettingDust/cloche/refs/heads/maven-repo/")
            maven("https://raw.githubusercontent.com/SettingDust/minecraft-codev/refs/heads/maven-repo/")
            maven("https://raw.githubusercontent.com/SettingDust/jvm-multiplatform/refs/heads/maven-repo/")
            mavenLocal()
        }
    }
}

object VersionFormats {
    val versionPlusMc = { mcVer: String, ver: String -> "$ver+$mcVer" }
    val mcDashVersion = { mcVer: String, ver: String -> "$mcVer-$ver" }
}

object VersionTransformers {
    val versionDashLoader = { ver: String, variant: String -> "$ver-$variant" }
    val loaderUnderlineVersion = { ver: String, variant: String -> "${variant}_$ver" }
}

object ArtifactTransformers {
    val artifactDashLoaderDashMcVersion =
        { artifact: String, variant: String, mcVersion: String -> "$artifact-$variant-$mcVersion" }
    val artifactDashLoader = { artifact: String, variant: String, _: String -> "$artifact-$variant" }
}

open class VariantConfig(
    val artifactTransformer: (artifact: String, variant: String, mcVersion: String) -> String = { artifact, _, _ -> artifact },
    val versionTransformer: (version: String, variant: String) -> String = { ver, _ -> ver }
) {
    companion object : VariantConfig()
}

data class VariantMapping(
    val mcVersion: String,
    val loaders: Map<String, VariantConfig>
)

fun VersionCatalogBuilder.modrinth(
    id: String,
    artifact: String = id,
    mcVersionToVersion: Map<String, String>,
    versionFormat: (String, String) -> String = { _, v -> v },
    mapping: List<VariantMapping> = emptyList()
) {
    val allLoaders = mapping.flatMap { it.loaders.keys }.toSet()
    val isSingleLoader = allLoaders.size == 1
    val isSingleMcVersion = mcVersionToVersion.size == 1

    if (isSingleMcVersion) {
        val (mcVersion, modVersion) = mcVersionToVersion.entries.single()
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, modVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (isSingleLoader) "$id"
                else "${id}_$loaderName",
                "maven.modrinth",
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
        return
    }

    mcVersionToVersion.forEach { (mcVersion, modVersion) ->
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, modVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (isSingleLoader) "${id}_${mcVersion}"
                else "${id}_${mcVersion}_$loaderName",
                "maven.modrinth",
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
    }
}

fun VersionCatalogBuilder.maven(
    id: String,
    group: String,
    artifact: String = id,
    mcVersionToVersion: Map<String, String>,
    versionFormat: (String, String) -> String = { _, v -> v },
    mapping: List<VariantMapping> = emptyList()
) {
    val allLoaders = mapping.flatMap { it.loaders.keys }.toSet()
    val isSingleLoader = allLoaders.size == 1
    val isSingleMcVersion = mcVersionToVersion.size == 1

    if (isSingleMcVersion) {
        val (mcVersion, modVersion) = mcVersionToVersion.entries.single()
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, modVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (isSingleLoader) id
                else "${id}_$loaderName",
                group,
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
        return
    }

    mcVersionToVersion.forEach { (mcVersion, baseVersion) ->
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, baseVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (mcVersion == "*") {
                    if (isSingleLoader) id
                    else "${id}_$loaderName"
                } else {
                    if (isSingleLoader) "${id}_${mcVersion}"
                    else "${id}_${mcVersion}_$loaderName"
                },
                group,
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
    }
}

dependencyResolutionManagement.versionCatalogs.create("catalog") {
    maven(
        id = "mixinextras",
        group = "io.github.llamalad7",
        artifact = "mixinextras",
        mcVersionToVersion = mapOf("*" to "0.5.0"),
        versionFormat = { _, v -> v },
        mapping = listOf(
            VariantMapping(
                "*", mapOf(
                    "forge" to VariantConfig(ArtifactTransformers.artifactDashLoader),
                    "fabric" to VariantConfig(ArtifactTransformers.artifactDashLoader),
                    "common" to VariantConfig(ArtifactTransformers.artifactDashLoader)
                )
            )
        )
    )

    library("preloadingTricks", "maven.modrinth", "preloading-tricks").version("2.5.10")

    library("kinecraft", "maven.modrinth", "kinecraft").version("2.2.1")

    library("reflect", "net.lenni0451", "Reflect").version("1.5.0")

    maven(
        id = "yacl",
        group = "dev.isxander",
        artifact = "yet-another-config-lib",
        mcVersionToVersion = mapOf(
            "1.20.1" to "3.6.6",
            "1.21.1" to "3.8.0"
        ),
        versionFormat = VersionFormats.versionPlusMc,
        mapping = listOf(
            VariantMapping(
                "1.20.1", mapOf(
                    "forge" to VariantConfig(versionTransformer = VersionTransformers.versionDashLoader),
                    "fabric" to VariantConfig(versionTransformer = VersionTransformers.versionDashLoader)
                )
            ),
            VariantMapping(
                "1.21.1", mapOf(
                    "neoforge" to VariantConfig(versionTransformer = VersionTransformers.versionDashLoader),
                    "fabric" to VariantConfig(versionTransformer = VersionTransformers.versionDashLoader)
                )
            )
        )
    )

    maven(
        id = "modmenu",
        group = "com.terraformersmc",
        artifact = "modmenu",
        mcVersionToVersion = mapOf(
            "1.20.1" to "7.2.2",
            "1.21.1" to "11.0.3"
        ),
        mapping = listOf(
            VariantMapping(
                "1.20.1", mapOf(
                    "fabric" to VariantConfig
                )
            ),
            VariantMapping(
                "1.21.1", mapOf(
                    "fabric" to VariantConfig
                )
            )
        )
    )
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ModSets"
