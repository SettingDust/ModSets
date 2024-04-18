architectury { forge() }

dependencies {
    forge(catalog.forge)

    api(project(":common:config"))
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
