architectury { forge() }

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

    mavenLocal()
}

dependencies {
    forge(catalog.forge)
    implementation(catalog.preloading.tricks)

    implementation(project(path = ":common:config", configuration = "namedElements")) {
        isTransitive = false
    }
}

tasks {
    jar {
        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
            )
        }
    }
}
