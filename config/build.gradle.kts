architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

dependencies {
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines)
    api(libs.kotlin.reflect)
}

tasks {
    jar {
        manifest.attributes(
            "FMLModType" to "LIBRARY",
        )
    }
}
