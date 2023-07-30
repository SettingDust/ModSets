import net.fabricmc.loom.build.nesting.IncludedJarFactory
import net.fabricmc.loom.task.AbstractRunTask

val archives_name: String by rootProject
val mod_name: String by rootProject

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
        )
    }
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

val commonCoreJar by tasks.registering(Jar::class) {
    archiveClassifier.set("common-core")
    from(project(":common").sourceSets.main.get().output)
    destinationDirectory.set(project.buildDir.resolve("devlibs"))
    manifest {
        attributes(
            "FMLModType" to "LIB",
        )
    }
}

loom {
    mods {
        named("main") {
//            modFiles.from(modJar)
//            sourceSet(project(":common").sourceSets.main.get())
//            sourceSet(sourceSets.named("core").get())
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
    forge(libs.neoforge)

    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge)

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)
    modRuntimeOnly(kinecraft)
}

tasks {
    remapJar {
        from(project(":common").sourceSets.main.get().output)
        forgeNestedJars.add(
            IncludedJarFactory.NestedFile(
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.forge",
                    "service",
                    version.toString(),
                    "service"
                ),
                modJar.get().outputs.files.singleFile
            ),
        )
        forgeNestedJars.add(
            IncludedJarFactory.NestedFile(
                IncludedJarFactory.Metadata(
                    "settingdust.modsets.forge.language",
                    "language",
                    version.toString(),
                    "language"
                ),
                languageJar.get().outputs.files.singleFile
            ),
        )
    }

    processResources {
        exclude("META-INF/mods.toml")
    }

    named<ProcessResources>("processCoreResources") {
        from(project(":common").sourceSets.main.get().resources)
    }

    afterEvaluate {
        withType<AbstractRunTask> {
            classpath = classpath.filter { it !in sourceSets.main.get().output }
            classpath += files(jar, modJar, languageJar)
        }
    }
}
