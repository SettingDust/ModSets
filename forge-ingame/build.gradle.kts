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

    implementation(project(path = ":config", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(project(path = ":ingame", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(project(":forge-mod-locator"))
}
