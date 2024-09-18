architectury { forge() }

dependencies {
    forge(catalog.forge)

    api(project(":common"))
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
