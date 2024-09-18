architectury { forge() }

repositories {
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
    mavenLocal()
}

dependencies {
    forge(catalog.forge)
    implementation(catalog.forge.kotlin)

    implementation(project(":common")) {
        isTransitive = false
    }

    implementation(project(":common:ingame")) {
        isTransitive = false
    }

    implementation(project(":forge:mod-locator"))
}
