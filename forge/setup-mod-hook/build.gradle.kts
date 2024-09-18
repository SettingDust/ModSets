plugins {
    alias(catalog.plugins.forge.gradle)
}

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

dependencies {
    minecraft(catalog.minecraft.forge)
    implementation(catalog.preloading.tricks)

    implementation(project(":common"))
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
