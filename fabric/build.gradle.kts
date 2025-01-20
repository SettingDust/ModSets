plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)
    alias(catalog.plugins.fabric.loom)
}

val archives_name: String by rootProject
val mod_name: String by rootProject

version = rootProject.version

loom {
    mixin { add("main", "$archives_name.fabric.refmap.json") }

    mods {
        create(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":common"))
            sourceSet("main", project(":common:ingame"))
            dependency(
                catalog.kotlinx.serialization.core.get(),
                catalog.kotlinx.serialization.json.get(),
                catalog.kotlinx.coroutines.get(),
                catalog.kotlin.reflect.get(),
            )
        }
    }

    runs { named("client") { ideConfigGenerated(true) } }
}

tasks { processResources { from(project(":common:ingame").sourceSets.main.get().resources) } }

dependencies {
    minecraft(catalog.minecraft)
    mappings(loom.officialMojangMappings())

    implementation(catalog.kotlinx.serialization.core)
    implementation(catalog.kotlinx.serialization.json)
    implementation(catalog.kotlinx.coroutines)
    implementation(catalog.kotlin.reflect)

    implementation(project(":common")) {
        isTransitive = false
    }
    include(project(":common"))

    implementation(project(":common:ingame")) {
        isTransitive = false
    }
    include(project(":fabric:ingame"))

    modImplementation(catalog.fabric.loader)
    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modImplementation(catalog.modmenu) { exclude(module = "fabric-loader") }

    modRuntimeOnly(variantOf(catalog.kinecraft.serialization) { classifier("fabric") })

    catalog.preloading.tricks.let {
        implementation(it)
        include(it)
    }
}

tasks {
    ideaSyncTask { enabled = true }
}
