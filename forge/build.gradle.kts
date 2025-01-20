import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace

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
    minecraft(catalog.lexforge)

    jarJar(project(":common")) {
        isTransitive = false
        jarJar.ranged(this, "[$version,)")
    }
    jarJar(project(":forge:ingame")) {
        isTransitive = false
        jarJar.ranged(this, "[$version,)")
    }

    jarJar(project(":forge:mod")) {
        isTransitive = false
        jarJar.ranged(this, "[$version,)")
    }
    jarJar(project(":forge:setup-mod-hook")) {
        isTransitive = false
        jarJar.ranged(this, "[$version,)")
    }

    shadow(project(":forge:mod-locator")) {
        isTransitive = false
    }

    runtimeOnly(catalog.kotlin.forge)
    runtimeOnly(fg.deobf(catalog.yacl.forge.get()))

    jarJar(catalog.preloading.tricks) {
        jarJar.ranged(this, "[$version,)")
    }
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
        archiveClassifier = "shadow"
    }

    afterEvaluate {
        named<RenameJarInPlace>("reobfJar") {
            input = shadowJar.flatMap { it.archiveFile }
        }
    }

    jar {
        from(this@tasks.jarJar.flatMap { it.archiveFile }.map { zipTree(it) })

        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
            )
        }
    }

    this.jarJar {
        dependsOn(":common:ingame:jar", ":forge:mod:reobfJar", ":forge:ingame:reobfJar")

        from(shadowJar.flatMap { it.archiveFile }.map { zipTree(it) })

        eachFile {
            if (path.startsWith("META-INF/jarjar")) {
                if (name != "metadata.json")
                    path = "META-INF/jars/$name"
                else {
                    filter {
                        it.replace("\"path\": \"META-INF/jarjar", "\"path\": \"META-INF/jars")
                    }
                }
            }
        }
    }
}
