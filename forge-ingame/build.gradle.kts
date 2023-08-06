import net.fabricmc.loom.task.AbstractRunTask

architectury {
    forge()
}

repositories {
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
    mavenLocal()
}

dependencies {
    forge(libs.forge)
    implementation(libs.kotlin.forge)

    implementation(project(path = ":config", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(project(path = ":ingame", configuration = "namedElements")) {
        isTransitive = false
    }

    implementation(project(":forge-mod-locator"))
}
