architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
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
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
}

dependencies {
    modApi(libs.yacl.common) {
        isTransitive = false
    }
    modApi(libs.modmenu)

    modApi("maven.modrinth:kinecraft-serialization:${libs.versions.kinecraft.serialization.get()}-fabric")

    api(project(":config"))
}

tasks {
    jar {
        manifest.attributes(
            "FMLModType" to "GAMELIBRARY",
        )
    }
}
