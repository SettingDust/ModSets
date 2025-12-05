@file:Suppress("UnstableApiUsage", "INVISIBLE_REFERENCE")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import earth.terrarium.cloche.INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE
import earth.terrarium.cloche.REMAPPED_ATTRIBUTE
import earth.terrarium.cloche.api.attributes.MinecraftModLoader
import earth.terrarium.cloche.api.attributes.TargetAttributes
import earth.terrarium.cloche.api.metadata.CommonMetadata
import earth.terrarium.cloche.api.metadata.FabricMetadata
import earth.terrarium.cloche.api.target.FabricTarget
import earth.terrarium.cloche.api.target.ForgeLikeTarget
import earth.terrarium.cloche.api.target.MinecraftTarget
import earth.terrarium.cloche.target.LazyConfigurableInternal
import earth.terrarium.cloche.tasks.GenerateFabricModJson
import groovy.lang.Closure
import net.msrandom.minecraftcodev.core.utils.lowerCamelCaseGradleName
import net.msrandom.minecraftcodev.fabric.MinecraftCodevFabricPlugin
import net.msrandom.minecraftcodev.fabric.task.JarInJar
import net.msrandom.minecraftcodev.forge.task.JarJar
import net.msrandom.minecraftcodev.runs.MinecraftRunConfiguration
import net.msrandom.virtualsourcesets.SourceSetStaticLinkageInfo
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.jvm.tasks.Jar
import java.nio.charset.StandardCharsets

plugins {
    java
    idea

    id("com.palantir.git-version") version "4.1.0"

    id("com.gradleup.shadow") version "9.2.2"

    id("earth.terrarium.cloche") version "0.16.21-dust"
}

val archive_name: String by rootProject.properties
val id: String by rootProject.properties
val source: String by rootProject.properties

group = "settingdust.mod_sets"

val gitVersion: Closure<String> by extra
version = gitVersion()

base { archivesName = archive_name }

repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        content {
            includeGroup("thedarkcolour")
        }
    }

    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"

        content {
            includeGroup("dev.isxander")
            includeGroup("org.quiltmc.parsers")
        }
    }

    maven("https://maven.terraformersmc.com/") {
        content {
            includeGroup("com.terraformersmc")
        }
    }

    mavenCentral()

    cloche {
        librariesMinecraft()
        main()
        mavenFabric()
        mavenForge()
        mavenNeoforged()
        mavenNeoforgedMeta()
        mavenParchment()
    }

    mavenLocal()
}

class MinecraftVersionCompatibilityRule : AttributeCompatibilityRule<String> {
    override fun execute(details: CompatibilityCheckDetails<String>) {
        details.compatible()
    }
}

class MinecraftModLoaderCompatibilityRule : AttributeCompatibilityRule<MinecraftModLoader> {
    override fun execute(details: CompatibilityCheckDetails<MinecraftModLoader>) {
        if (details.producerValue == MinecraftModLoader.common) {
            details.compatible()
        }
    }
}

dependencies {
    attributesSchema {
        attribute(TargetAttributes.MINECRAFT_VERSION) {
            compatibilityRules.add(MinecraftVersionCompatibilityRule::class)
        }
        attribute(TargetAttributes.MOD_LOADER) {
            compatibilityRules.add(MinecraftModLoaderCompatibilityRule::class)
        }
        attribute(TargetAttributes.CLOCHE_MINECRAFT_VERSION) {
            compatibilityRules.add(MinecraftVersionCompatibilityRule::class)
        }
        attribute(TargetAttributes.CLOCHE_MOD_LOADER) {
            compatibilityRules.add(MinecraftModLoaderCompatibilityRule::class)
        }
    }
}

cloche {
    metadata {
        modId = id
        name = rootProject.property("name").toString()
        description = rootProject.property("description").toString()
        license = "Apache License 2.0"
        icon = "assets/$id/icon.png"
        sources = source
        issues = "$source/issues"
        author("SettingDust")

        dependency {
            modId = "minecraft"
            type = CommonMetadata.Dependency.Type.Required
            version {
                start = "1.20.1"
            }
        }

        dependency {
            modId = "yet_another_config_lib_v3"
            type = CommonMetadata.Dependency.Type.Required
        }
    }

    mappings {
        official()
    }

    common()

    val commonMain = common("common:common") {
        configurations.named("commonCommonRuntimeElements") {
            attributes {
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
            }
        }
    }

    val common1201 = common("common:1.20.1") {
        dependsOn(commonMain)
        // mixins.from("src/common/1.20.1/main/resources/$id.1_20.mixins.json")
    }
    val common121 = common("common:1.21.1") {
        dependsOn(commonMain)
        // mixins.from("src/common/1.21.1/main/resources/$id.1_21.mixins.json")
    }

    val commonGame = common("common:game") {
        project.dependencies {
            val implementation = lowerCamelCaseGradleName(name, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

            implementation(project(":")) {
                capabilities {
                    requireFeature(commonMain.capabilitySuffix)
                }
            }
        }
    }

    val commonGame1201 = common("common:game:1.20.1") {
        dependsOn(commonGame)
        // mixins.from("src/common/1.20.1/main/resources/$id.1_20.mixins.json")
    }
    val commonGame121 = common("common:game:1.21.1") {
        dependsOn(commonGame)
        // mixins.from("src/common/1.21.1/main/resources/$id.1_21.mixins.json")
    }

    run fabric@{
        val fabricCommon = common("fabric:common") {
            dependsOn(commonGame, commonMain)

            // mixins.from(file("src/fabric/common/main/resources/$id.fabric.mixins.json"))
        }

        val fabric1201 = fabric("fabric:1.20.1") {
            dependsOn(commonMain, common1201, commonGame, commonGame1201)

            sourceSet.the<SourceSetStaticLinkageInfo>().weakTreeLink(commonGame.sourceSet, commonMain.sourceSet)

            minecraftVersion = "1.20.1"

            runs { client() }

            metadata {
                name = cloche.metadata.name.map { "$it 1.20" }

                dependency {
                    modId = "minecraft"
                    type = CommonMetadata.Dependency.Type.Required
                    version {
                        start = "1.20.1"
                        end = "1.21"
                    }
                }

                custom("modmenu" to mapOf("parent" to id))
            }

            dependencies {
                fabricApi("0.92.6")

                modImplementation(catalog.yacl.get1().get20().get1().fabric)

                modImplementation(catalog.modmenu.get1().get20().get1())
            }

            tasks.named<GenerateFabricModJson>(generateModsManifestTaskName) {
                modId = "${id}_1_20"
            }
        }

        val fabric121 = fabric("fabric:1.21") {
            dependsOn(commonMain, common121, commonGame, commonGame121)

            sourceSet.the<SourceSetStaticLinkageInfo>().weakTreeLink(commonGame.sourceSet, commonMain.sourceSet)

            minecraftVersion = "1.21.1"

            runs { client() }

            metadata {
                name = cloche.metadata.name.map { "$it 1.21" }

                dependency {
                    modId = "minecraft"
                    type = CommonMetadata.Dependency.Type.Required
                    version {
                        start = "1.21"
                    }
                }

                custom("modmenu" to mapOf("parent" to id))
            }

            dependencies {
                fabricApi("0.116.6")

                modImplementation(catalog.yacl.get1().get21().get1().fabric)

                modImplementation(catalog.modmenu.get1().get21().get1())
            }

            tasks.named<GenerateFabricModJson>(generateModsManifestTaskName) {
                modId = "${id}_1_21"
            }
        }

        run container@{
            val featureName = "containerFabric"
            val metadataDirectory = project.layout.buildDirectory.dir("generated")
                .map { it.dir("metadata").dir(featureName) }
            val include = configurations.register(lowerCamelCaseGradleName(featureName, "include")) {
                isCanBeResolved = true
                isTransitive = false

                attributes {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                    attribute(REMAPPED_ATTRIBUTE, false)
                    attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                }
            }
            val targets = setOf(fabric1201, fabric121)

            dependencies {
                for (target in targets) {
                    include(project(":")) {
                        capabilities {
                            requireFeature(target.capabilitySuffix!!)
                        }
                    }
                }
            }

            tasks {
                val generateModJson =
                    register<GenerateFabricModJson>(lowerCamelCaseGradleName(featureName, "generateModJson")) {
                        modId = id
                        metadata = objects.newInstance(FabricMetadata::class.java, fabric1201).apply {
                            license.value(cloche.metadata.license)
                            dependencies.value(cloche.metadata.dependencies)
                        }
                        loaderDependencyVersion = "0.17"
                        output.set(metadataDirectory.map { it.file("fabric.mod.json") })
                    }

                val jar = register<Jar>(lowerCamelCaseGradleName(featureName, "jar")) {
                    group = "build"
                    archiveClassifier = "fabric"
                    destinationDirectory = intermediateOutputsDirectory
                    dependsOn(generateModJson)
                    from(metadataDirectory)
                }

                val includesJar = register<JarInJar>(lowerCamelCaseGradleName(featureName, "includeJar")) {
                    dependsOn(targets.map { it.includeJarTaskName })

                    archiveClassifier = "fabric"
                    input = jar.flatMap { it.archiveFile }
                    fromResolutionResults(include)
                }

                build {
                    dependsOn(includesJar)
                }
            }
        }

        targets.withType<FabricTarget> {
            loaderVersion = "0.18.1"

            includedClient()

            dependsOn(fabricCommon)

            metadata {
                entrypoint("main") {
                    value = "$group.fabric.ModSetsFabric::init"
                }

                entrypoint("modmenu") {
                    value = "$group.fabric.ModSetsModMenu"
                }

                dependency {
                    modId = "fabric-api"
                    type = CommonMetadata.Dependency.Type.Required
                }
            }
        }
    }

    run forge@{
        val forgeService = forge("forge:service") {
            dependsOn(commonMain, common1201)

            minecraftVersion = "1.20.1"
            loaderVersion = "47.4.4"

            tasks {
                named(generateModsTomlTaskName) {
                    enabled = false
                }
            }
        }

        val forgeGame = forge("forge:game") {
            dependsOn(commonGame, commonGame1201)

            minecraftVersion = "1.20.1"
            loaderVersion = "47.4.4"

            metadata {
                dependency {
                    modId = "minecraft"
                    type = CommonMetadata.Dependency.Type.Required
                    version {
                        start = "1.20.1"
                        end = "1.21"
                    }
                }

                dependency {
                    modId = "preloading_tricks"
                    type = CommonMetadata.Dependency.Type.Required
                }
            }

            repositories {
                maven("https://repo.spongepowered.org/maven") {
                    content {
                        includeGroup("org.spongepowered")
                    }
                }
            }

            dependencies {
                implementation("org.spongepowered:mixin:0.8.7")
                compileOnly(catalog.mixinextras.common)
                implementation(catalog.mixinextras.forge)

                modImplementation(catalog.yacl.get1().get20().get1().forge)

                implementation(project(":")) {
                    capabilities {
                        requireFeature(forgeService.capabilitySuffix!!)
                    }
                }

                implementation(project(":")) {
                    capabilities {
                        requireFeature(commonMain.capabilitySuffix)
                    }
                }
            }

            tasks {
                named<Jar>(lowerCamelCaseGradleName(featureName, "jar")) {
                    manifest {
                        attributes(
                            "ForgeVariant" to "LexForge"
                        )
                    }
                }

                named(accessWidenTaskName) {
                    dependsOn(forgeService.accessWidenTaskName)
                }
            }
        }

        forge("version:forge:1.20.1") {
            minecraftVersion = "1.20.1"
            loaderVersion = "47.4.4"

            runs {
                client {
                    env("MOD_CLASSES", "")
                }
            }

            dependencies {
                implementation(project(":")) {
                    capabilities {
                        requireFeature(forgeService.capabilitySuffix!!)
                    }
                }
                implementation(project(":")) {
                    capabilities {
                        requireFeature(forgeGame.capabilitySuffix!!)
                    }
                }
            }

            tasks {
                named(jarTaskName) {
                    enabled = false
                }

                named(remapJarTaskName) {
                    enabled = false
                }

                named(includeJarTaskName) {
                    enabled = false
                }

                named(accessWidenTaskName) {
                    dependsOn(forgeService.accessWidenTaskName, forgeGame.accessWidenTaskName)
                }
            }
        }

        run container@{
            val featureName = "containerForge"

            val include = configurations.register(lowerCamelCaseGradleName(featureName, "include")) {
                isCanBeResolved = true
                isTransitive = false

                attributes {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                    attribute(REMAPPED_ATTRIBUTE, false)
                    attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                }
            }

            val embed = configurations.register(lowerCamelCaseGradleName(featureName, "embed")) {
                isCanBeResolved = true
                isTransitive = false

                attributes {
                    attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.CLASSES_AND_RESOURCES)
                    )
                    attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                }
            }

            dependencies {
                include(project(":")) {
                    capabilities {
                        requireFeature(forgeGame.capabilitySuffix!!)
                    }
                }
            }

            project.dependencies {
                embed(project(":")) {
                    capabilities {
                        requireFeature(forgeService.capabilitySuffix!!)
                    }
                }
            }

            tasks {
                val jar = register<Jar>(lowerCamelCaseGradleName(featureName, "jar")) {
                    group = "build"

                    archiveClassifier = "forge"
                    destinationDirectory = intermediateOutputsDirectory

                    from(embed)
                }

                val includesJar = register<JarJar>(lowerCamelCaseGradleName(featureName, "includeJar")) {
                    group = "build"
                    dependsOn(targets.map { it.includeJarTaskName })

                    archiveClassifier = "forge"
                    input = jar.flatMap { it.archiveFile }
                    fromResolutionResults(include)
                }

                build {
                    dependsOn(includesJar)
                }
            }
        }
    }

    run neoforge@{
        val neoforgeService = neoforge("neoforge:service") {
            dependsOn(commonMain, common121)

            minecraftVersion = "1.21.1"
            loaderVersion = "21.1.192"

            tasks {
                named(generateModsTomlTaskName) {
                    enabled = false
                }
            }
        }

        val neoforgeGame = neoforge("neoforge:game") {
            dependsOn(commonGame, commonGame121)

            minecraftVersion = "1.21.1"
            loaderVersion = "21.1.192"

            metadata {
                dependency {
                    modId = "minecraft"
                    type = CommonMetadata.Dependency.Type.Required
                    version {
                        start = "1.21"
                    }
                }

                dependency {
                    modId = "preloading_tricks"
                    type = CommonMetadata.Dependency.Type.Required
                }
            }

            dependencies {
                modImplementation(catalog.yacl.get1().get21().get1().neoforge)

                implementation(project(":")) {
                    capabilities {
                        requireFeature(neoforgeService.capabilitySuffix!!)
                    }
                }

                implementation(project(":")) {
                    capabilities {
                        requireFeature(commonMain.capabilitySuffix)
                    }
                }
            }

            tasks {
                named<Jar>(lowerCamelCaseGradleName(featureName, "jar")) {
                    manifest {
                        attributes(
                            "ForgeVariant" to "NeoForge"
                        )
                    }
                }

                named(accessWidenTaskName) {
                    dependsOn(neoforgeService.accessWidenTaskName)
                }
            }
        }

        neoforge("version:neoforge:1.21") {
            minecraftVersion = "1.21.1"
            loaderVersion = "21.1.192"

            runs {
                client {
                    env("MOD_CLASSES", "")
                }
            }

            dependencies {
                legacyClasspath(project(":")) {
                    capabilities {
                        requireFeature(neoforgeService.capabilitySuffix!!)
                    }

                    attributes {
                        attribute(REMAPPED_ATTRIBUTE, false)
                        attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                    }
                }
                modImplementation(project(":")) {
                    capabilities {
                        requireFeature(neoforgeService.capabilitySuffix!!)
                    }
                }
                modImplementation(project(":")) {
                    capabilities {
                        requireFeature(neoforgeGame.capabilitySuffix!!)
                    }
                }
                legacyClasspath(catalog.preloadingTricks)
            }

            tasks {
                named(jarTaskName) {
                    enabled = false
                }

                named(remapJarTaskName) {
                    enabled = false
                }

                named(includeJarTaskName) {
                    enabled = false
                }

                named(accessWidenTaskName) {
                    dependsOn(neoforgeService.accessWidenTaskName, neoforgeGame.accessWidenTaskName)
                }
            }
        }

        neoforge("version:neoforge:1.21.10") {
            minecraftVersion = "1.21.10"
            loaderVersion = "21.10.38-beta"

            runs {
                client {
                    env("MOD_CLASSES", "")
                }
            }

            dependencies {
                modImplementation(catalog.yacl.get1().get21().get9().neoforge)

                modImplementation(project(":")) {
                    capabilities {
                        requireFeature(neoforgeService.capabilitySuffix!!)
                    }
                }
                modImplementation(project(":")) {
                    capabilities {
                        requireFeature(neoforgeGame.capabilitySuffix!!)
                    }
                }
            }

            tasks {
                named(generateModsTomlTaskName) {
                    enabled = false
                }

                named(jarTaskName) {
                    enabled = false
                }

                named(remapJarTaskName) {
                    enabled = false
                }

                named(includeJarTaskName) {
                    enabled = false
                }

                named(accessWidenTaskName) {
                    dependsOn(neoforgeService.accessWidenTaskName, neoforgeGame.accessWidenTaskName)
                }
            }
        }

        run container@{
            val featureName = "containerNeoforge"

            val include = configurations.register(lowerCamelCaseGradleName(featureName, "include")) {
                isCanBeResolved = true
                isTransitive = false

                attributes {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                    attribute(REMAPPED_ATTRIBUTE, false)
                    attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                }
            }

            val embed = configurations.register(lowerCamelCaseGradleName(featureName, "embed")) {
                isCanBeResolved = true
                isTransitive = false

                attributes {
                    attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.CLASSES_AND_RESOURCES)
                    )
                    attribute(INCLUDE_TRANSFORMED_OUTPUT_ATTRIBUTE, false)
                }
            }

            dependencies {
                include(project(":")) {
                    capabilities {
                        requireFeature(neoforgeGame.capabilitySuffix!!)
                    }
                }
            }

            project.dependencies {
                embed(project(":")) {
                    capabilities {
                        requireFeature(neoforgeService.capabilitySuffix!!)
                    }
                }
            }

            tasks {
                val jar = register<Jar>(lowerCamelCaseGradleName(featureName, "jar")) {
                    group = "build"

                    archiveClassifier = "neoforge"
                    destinationDirectory = intermediateOutputsDirectory

                    from(embed)
                }

                val includesJar = register<JarJar>(lowerCamelCaseGradleName(featureName, "includeJar")) {
                    group = "build"
                    dependsOn(targets.map { it.includeJarTaskName })

                    archiveClassifier = "neoforge"
                    input = jar.flatMap { it.archiveFile }
                    fromResolutionResults(include)
                }

                build {
                    dependsOn(includesJar)
                }
            }
        }
    }

    targets.all {
        runs {
            (client as LazyConfigurableInternal<MinecraftRunConfiguration>).onConfigured {
                it.jvmArguments(
                    "-Dmixin.debug.verbose=true",
                    "-Dmixin.debug.export=true",
                    "-Dclasstransform.dumpClasses=true"
                )
            }
        }

        mappings {
            parchment(minecraftVersion.map {
                when (it) {
                    "1.20.1" -> "2023.09.03"
                    "1.21.1" -> "2024.11.17"
                    "1.21.10" -> "2025.10.12"
                    else -> throw IllegalArgumentException("Unsupported minecraft version $it")
                }
            })
        }

        dependencies {
            implementation(extractIncludes(catalog.preloadingTricks))
        }
    }
}

val SourceSet.includeJarTaskName: String
    get() = lowerCamelCaseGradleName(takeUnless(SourceSet::isMain)?.name, "includeJar")

val MinecraftTarget.includeJarTaskName: String
    get() = when (this) {
        is FabricTarget -> sourceSet.includeJarTaskName
        is ForgeLikeTarget -> sourceSet.includeJarTaskName
        else -> throw IllegalArgumentException("Unsupported target $this")
    }

val FabricTarget.generateModsJsonTaskName: String
    get() = lowerCamelCaseGradleName("generate", featureName, "ModJson")

val ForgeLikeTarget.generateModsTomlTaskName: String
    get() = lowerCamelCaseGradleName("generate", featureName, "modsToml")

val MinecraftTarget.generateModsManifestTaskName: String
    get() = when (this) {
        is FabricTarget -> generateModsJsonTaskName
        is ForgeLikeTarget -> generateModsTomlTaskName
        else -> throw IllegalArgumentException("Unsupported target $this")
    }

val MinecraftTarget.jarTaskName: String
    get() = lowerCamelCaseGradleName(featureName, "jar")

val MinecraftTarget.remapJarTaskName: String
    get() = lowerCamelCaseGradleName(featureName, "remapJar")

val MinecraftTarget.accessWidenTaskName: String
    get() = lowerCamelCaseGradleName("accessWiden", featureName, "minecraft")

val MinecraftTarget.decompileMinecraftTaskName: String
    get() = lowerCamelCaseGradleName("decompile", featureName, "minecraft")

tasks {
    withType<ProcessResources> {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    shadowJar {
        enabled = false
    }

    val shadowContainersJar by registering(ShadowJar::class) {
        archiveClassifier = ""

        val fabricJar = project.tasks.named<Jar>(lowerCamelCaseGradleName("containerFabric", "includeJar"))
        from(fabricJar.map { zipTree(it.archiveFile) })
        manifest.from(fabricJar.get().manifest)

        val forgeJar = project.tasks.named<Jar>(lowerCamelCaseGradleName("containerForge", "includeJar"))
        from(forgeJar.map { zipTree(it.archiveFile) })
        manifest.from(forgeJar.get().manifest)

        val neoforgeJar =
            project.tasks.named<Jar>(lowerCamelCaseGradleName("containerNeoforge", "includeJar"))
        from(neoforgeJar.map { zipTree(it.archiveFile) }) {
            include("settingdust/mod_sets/neoforge/**/*")
            include("META-INF/services/*")
            include("META-INF/jarjar/*")
            include("META-INF/jars/*")
        }

        append("META-INF/accesstransformer.cfg")

        mergeServiceFiles()

        transform(object : ResourceTransformer {
            private val gson = GsonBuilder().setPrettyPrinting().create()
            private val collected = JsonArray()
            private val path = "META-INF/jarjar/metadata.json"
            private var transformed = false

            override fun canTransformResource(element: FileTreeElement): Boolean {
                return element.path == path
            }

            override fun transform(context: TransformerContext) {
                context.inputStream.use { input ->
                    val json = gson.fromJson(input.reader(Charsets.UTF_8), JsonObject::class.java)
                    val jars = json.getAsJsonArray("jars")
                    jars?.forEach { collected.add(it) }
                    transformed = true
                }
            }

            override fun hasTransformedResource(): Boolean = transformed

            override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
                if (collected.size() == 0) return

                val merged = JsonObject().apply {
                    add("jars", collected)
                }

                os.putNextEntry(ZipEntry(path))
                os.write(gson.toJson(merged).toByteArray(StandardCharsets.UTF_8))
                os.closeEntry()
            }
        })
    }

    val shadowSourcesJar by registering(ShadowJar::class) {
        dependsOn(cloche.targets.map { it.generateModsManifestTaskName })

        mergeServiceFiles()
        archiveClassifier.set("sources")
        from(sourceSets.map { it.allSource })

        doFirst {
            manifest {
                from(source.filter { it.name.equals("MANIFEST.MF") }.toList())
            }
        }
    }

    build {
        dependsOn(shadowContainersJar, shadowSourcesJar)
    }

    for (target in cloche.targets.filterIsInstance<FabricTarget>()) {
        named(lowerCamelCaseGradleName("accessWiden", target.featureName, "commonMinecraft")) {
            dependsOn(
                lowerCamelCaseGradleName(
                    "remap",
                    target.featureName,
                    "commonMinecraft",
                    MinecraftCodevFabricPlugin.INTERMEDIARY_MAPPINGS_NAMESPACE,
                ), lowerCamelCaseGradleName(
                    "remap",
                    target.featureName,
                    "clientMinecraft",
                    MinecraftCodevFabricPlugin.INTERMEDIARY_MAPPINGS_NAMESPACE,
                ), lowerCamelCaseGradleName("generate", target.featureName, "MappingsArtifact")
            )
        }

        named(lowerCamelCaseGradleName("accessWiden", target.featureName, "Minecraft")) {
            dependsOn(
                lowerCamelCaseGradleName(
                    "remap",
                    target.featureName,
                    "clientMinecraft",
                    MinecraftCodevFabricPlugin.INTERMEDIARY_MAPPINGS_NAMESPACE,
                ), lowerCamelCaseGradleName("generate", target.featureName, "MappingsArtifact")
            )
        }
    }
}

gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        val deps = task.taskDependencies.getDependencies(task)
        if (deps.isNotEmpty()) {
            println("Task ${task.path} depends on:")
            deps.forEach { dep ->
                println("    - ${dep.path}")
            }
        }
    }
}
