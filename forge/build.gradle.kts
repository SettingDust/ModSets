import net.fabricmc.loom.build.nesting.IncludedJarFactory
import net.fabricmc.loom.task.AbstractRunTask
import net.fabricmc.loom.task.RemapJarTask

val archives_name: String by rootProject
val mod_name: String by rootProject

plugins {
    alias(libs.plugins.minotaur)
}

architectury {
    platformSetupLoomIde()
    forge()
}

sourceSets {
    val core by registering {
        compileClasspath += main.get().compileClasspath
        compileClasspath += main.get().output
    }

    val language by registering {
        compileClasspath += main.get().compileClasspath
        compileClasspath += main.get().output
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

    // Add KFF Maven repository
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }

    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases")
}

val modJar by tasks.registering(Jar::class) {
    archiveClassifier.set("core")
    from(sourceSets.named("core").get().output)
    destinationDirectory.set(project.buildDir.resolve("devlibs"))
    manifest {
        attributes(
            "MixinConfigs" to "$archives_name.mixins.json",
            "FMLModType" to "MOD",
        )
    }
}

val remapModJar by tasks.registering(RemapJarTask::class) {
    dependsOn(modJar)
    archiveClassifier.set("core")
    inputFile.convention(modJar.get().archiveFile)
    nestedJars.setFrom()
    forgeNestedJars.empty()
}

val languageJar by tasks.registering(Jar::class) {
    archiveClassifier.set("language")
    from(sourceSets.named("language").get().output)
    destinationDirectory.set(project.buildDir.resolve("devlibs"))
    manifest {
        attributes(
            "FMLModType" to "LANGPROVIDER",
        )
    }
}

val remapCommonModJar by tasks.registering(RemapJarTask::class) {
    dependsOn(project(":common").tasks.named("modJar"))
    archiveBaseName.set(project(":common").base.archivesName)
    archiveClassifier.set("game")
    inputFile.convention(project.project(":common").tasks.named<Jar>("modJar").get().archiveFile)
    nestedJars.setFrom()
    forgeNestedJars.empty()
}

loom {
    mods {
        named("main") {
//            modFiles.from(modJar)
//            sourceSet(project(":common").sourceSets.main.get())
//            sourceSet(sourceSets.named("core").get())
            modSourceSets.empty()
        }
    }

//    forge {
//        mixinConfig("$archives_name-common.mixins.json")
//        mixinConfig("$archives_name.mixins.json")
//    }

    runs {
        named("client") {
            property("mixin.debug.export", "true")
            property("mixin.debug.verbose", "true")
        }
    }
}

dependencies {
    forge(libs.forge)

    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(path = ":common", configuration = "transformProductionForge"))
    implementation(project.project(":common").sourceSets.named("game").get().output)

    implementation(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge)

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)
    modRuntimeOnly(kinecraft)
}

tasks {
//    processResources {
//        exclude("META-INF/mods.toml")
//    }

    remapJar {
        dependsOn(remapModJar, languageJar, remapCommonModJar)
//        from(project(":common").sourceSets.main.get().output)
        forgeNestedJars.add(
            IncludedJarFactory.NestedFile(
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.forge",
                    "mod_sets_forge_core",
                    version.toString(),
                    "core"
                ),
                remapModJar.get().archiveFile.get().asFile
            ),
        )
        forgeNestedJars.add(
            IncludedJarFactory.NestedFile(
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.forge.language",
                    "mod_sets_forge_language",
                    version.toString(),
                    "language"
                ),
                languageJar.get().archiveFile.get().asFile
            ),
        )
        forgeNestedJars.add(
            IncludedJarFactory.NestedFile(
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.common",
                    "mod_sets_common_game",
                    version.toString(),
                    "game"
                ),
                remapCommonModJar.get().archiveFile.get().asFile
            ),
        )
    }

    named<ProcessResources>("processCoreResources") {
        from(project(":common").sourceSets.main.get().resources)
    }

    afterEvaluate {
        withType<AbstractRunTask> {
            classpath =
                classpath.filter {
                    (it !in sourceSets.main.get().output)
                            && (it !in project(":common").tasks.jar.get().outputs.files)
                            && (it !in project(":common").sourceSets.named("game").get().output)
                }
            classpath += files(remapJar)
        }
    }
}

modrinth {
    token.set(env.MODRINTH_TOKEN.value) // This is the default. Remember to have the MODRINTH_TOKEN environment variable set or else this will fail, or set it to whatever you want - just make sure it stays private!
    projectId.set("mod-sets") // This can be the project ID or the slug. Either will work!
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(tasks.remapJar) // With Loom, this MUST be set to `remapJar` instead of `jar`!
    changelog.set(rootProject.file("CHANGELOG.md").readText())
    versionNumber.set("$version-forge")
    gameVersions.addAll(
        "1.19.4",
        "1.20",
        "1.20.1",
    ) // Must be an array, even with only one version
    loaders.add("forge") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
    dependencies {
        required.project("kotlin-for-forge")
        // https://modrinth.com/mod/yacl
        required.project("yacl")
        // https://modrinth.com/mod/kinecraft-serialization
        embedded.version("kinecraft-serialization", "${libs.versions.kinecraft.serialization.get()}-forge")
    }
}
