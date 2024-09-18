plugins {
    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)
    alias(catalog.plugins.forge.gradle)
}

minecraft {
    mappings("official", catalog.versions.minecraft.get())
}

dependencies {
    minecraft(catalog.minecraft.forge)
    implementation(catalog.kotlin.forge)

    implementation(project(":common")) {
        isTransitive = false
    }

    implementation(project(":common:ingame")) {
        isTransitive = false
    }

    implementation(project(":forge:mod-locator"))
}
