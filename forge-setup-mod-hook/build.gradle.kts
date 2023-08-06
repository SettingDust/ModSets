import net.fabricmc.loom.task.AbstractRunTask

architectury {
    forge()
}

repositories {
    mavenLocal()
}

dependencies {
    forge(libs.forge)
    implementation(libs.preloading.tricks.local)

    implementation(project(path = ":config", configuration = "namedElements")) {
        isTransitive = false
    }
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
