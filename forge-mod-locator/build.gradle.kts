architectury {
    forge()
}

dependencies {
    forge(libs.forge)

    api(project(":config"))
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
