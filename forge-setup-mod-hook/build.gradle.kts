architectury {
    forge()
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

    mavenLocal()
}

dependencies {
    forge(libs.forge)
    implementation(libs.preloading.tricks)

    implementation(project(path = ":config", configuration = "namedElements")) {
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
