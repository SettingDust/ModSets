plugins {
    alias(catalog.plugins.shadow)
    alias(catalog.plugins.forge.gradle)
}

val archives_name: String by rootProject
val mod_name: String by rootProject

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

jarJar.enable()

dependencies {
    minecraft(catalog.minecraft.forge)

    jarJar(project(":common")) {
        jarJar.ranged(this, "[$version,)")
    }
    jarJar(project(":forge:ingame")) {
        jarJar.ranged(this, "[$version,)")
    }

    jarJar(project(":forge:mod")) {
        jarJar.ranged(this, "[$version,)")
    }
    jarJar(project(":forge:setup-mod-hook")) {
        jarJar.ranged(this, "[$version,)")
    }

    shadow(project(":forge:mod-locator")) {
        isTransitive = false
    }

    runtimeOnly(catalog.kotlin.forge)
    runtimeOnly(fg.deobf(catalog.yacl.forge.get()))

    jarJar(catalog.kinecraft.serialization) {
        jarJar.ranged(this, "[$version,)")
    }
    jarJar(catalog.preloading.tricks) {
        jarJar.ranged(this, "[$version,)")
    }
}

tasks {
    jar { enabled = false }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
            )
        }
    }
}
