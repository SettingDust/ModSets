plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.quilt.loom)
}

val archives_name: String by rootProject


loom {
    mods {
        register(archives_name) {
            sourceSet("main")
            sourceSet("main", project(":common"))
            sourceSet("main", project(":common:ingame"))
        }
    }

    runs {
        named("client") {
            ideConfigGenerated(true)
        }
    }
}

dependencies {
    minecraft(catalog.minecraft.fabric)
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
    include(project(":common:ingame"))

    modImplementation(catalog.quilt.loader)
    modImplementation(catalog.quilt.standard.libraries.core)

    modRuntimeOnly(catalog.fabric.kotlin) { exclude(module = "fabric-loader") }

    modRuntimeOnly(catalog.yacl.fabric) { isTransitive = false }
    modImplementation(catalog.modmenu) { isTransitive = false }

    modRuntimeOnly(catalog.quilt.fabric.api)

    modRuntimeOnly(variantOf(catalog.kinecraft.serialization) { classifier("fabric") })
    include(catalog.kinecraft.serialization)

    catalog.preloading.tricks.let {
        implementation(it)
        include(it)
    }
}

tasks { processResources { from(project(":common:ingame").sourceSets.main.get().resources) } }
