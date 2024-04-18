architectury { forge() }

dependencies {
    forge(catalog.forge)

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
