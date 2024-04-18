architectury { common(rootProject.property("enabled_platforms").toString().split(",")) }

dependencies {
    api(catalog.kotlinx.serialization.core)
    api(catalog.kotlinx.serialization.json)
    api(catalog.kotlinx.coroutines)
    api(catalog.kotlin.reflect)
}

tasks {
    jar {
        manifest.attributes(
            "FMLModType" to "LIBRARY",
        )
    }
}
