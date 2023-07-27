val archives_name: String by rootProject
val mod_name: String by rootProject

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
//    mods {
//        register(archives_name) {
//            modFiles.from("../common/build/devlibs/${project(":common").base.archivesName.get()}-$version-dev.jar")
//            sourceSet(sourceSets.main.get())
//        }
//    }
    forge {
//        mixinConfig("$archives_name-common.mixins.json")
        mixinConfig("$archives_name.mixins.json")
    }

    runs {
        named("client") {
            property("mixin.debug.export", "true")
            property("mixin.debug.verbose", "true")
        }
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

dependencies {
    forge(libs.forge)

    implementation(project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }
    include(project(path = ":common", configuration = "transformProductionForge"))

    implementation(libs.kotlin.forge)
    modRuntimeOnly(libs.yacl.forge)

    val kinecraft = "maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-forge"
    include(kinecraft)
    modRuntimeOnly(kinecraft)
}

tasks {
    processResources {
        from(project(":common").sourceSets.main.get().resources)
    }
}
