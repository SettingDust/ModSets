architectury { common(rootProject.property("enabled_platforms").toString().split(",")) }

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
}

dependencies {
    modApi(catalog.yacl.common) { isTransitive = false }
    modApi(catalog.modmenu)

    modApi(
        "maven.modrinth:kinecraft-serialization:${catalog.kinecraft.serialization.get().version}-fabric"
    )

    api(project(":config"))
}

tasks {
    jar {
        manifest.attributes(
            "FMLModType" to "GAMELIBRARY",
        )
    }
}
