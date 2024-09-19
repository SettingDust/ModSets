import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.apache.commons.io.FileUtils

plugins {
    alias(catalog.plugins.forge.gradle)
}

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

dependencies {
    minecraft(catalog.minecraft.forge)
    implementation(project(":common"))
}

tasks {
    afterEvaluate {
        named<RenameJarInPlace>("reobfJar") {
            val ingameJarTask = project(":common:ingame").tasks.jar
            dependsOn(ingameJarTask)
            input = ingameJarTask.flatMap { it.archiveFile }.map { jar ->
                val output = project.layout.buildDirectory.dir("libs")
                    .flatMap { libs -> libs.file(tasks.jar.flatMap { forgeJar -> forgeJar.archiveFile.map { it.asFile.name } }) }
                    .get()
                FileUtils.copyFile(jar.asFile, output.asFile)
                jar
            }
        }
    }
}
