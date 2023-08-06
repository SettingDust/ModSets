architectury {
    forge()
}

dependencies {
    forge(libs.forge)
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
