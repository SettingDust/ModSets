package settingdust.modsets.fabric

import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.loader.impl.discovery.*
import net.fabricmc.loader.impl.gui.FabricGuiEntry
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.metadata.DependencyOverrides
import net.fabricmc.loader.impl.metadata.VersionOverrides
import net.fabricmc.loader.impl.util.SystemProperties
import org.slf4j.LoggerFactory
import settingdust.preloadingtricks.SetupModCallback
import settingdust.preloadingtricks.fabric.FabricModSetupService
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class FabricSetupModCallback : SetupModCallback {
    private val logger = LoggerFactory.getLogger("ModSets/SetupMod")
    private val service = FabricModSetupService.INSTANCE
    private var envDisabledMods = mutableMapOf<String, Set<ModCandidate>>()

    @Suppress("UNCHECKED_CAST")
    private val adapterMapProperty =
        FabricLoaderImpl::class.memberProperties.single { it.name == "adapterMap" } as KProperty<MutableMap<String, LanguageAdapter>>

    @Suppress("UNCHECKED_CAST")
    private val addModFunction =
        FabricLoaderImpl::class.functions.single { it.name == "addMod" } as KFunction<Void>

    @Suppress("UNCHECKED_CAST")
    private val addCandidateFinderFunction =
        ModDiscoverer::class.functions.single { it.name == "addCandidateFinder" } as KFunction<Void>


    init {
        adapterMapProperty.isAccessible = true
        addModFunction.isAccessible = true

        val candidates = try {
            discoverMods().resolveMods()
        } catch (e: ModResolutionException) {
            FabricGuiEntry.displayCriticalError(e, true)
            emptyList() // unreachable
        }
        logger.info(
            "Loading {} additional mod{}{}",
            candidates.size,
            if (candidates.size > 1) "s" else "",
            if (candidates.isEmpty()) {
                ""
            } else {
                ":\n ${candidates.joinToString("\n") { "\t- ${it.id} ${it.version.friendlyString}" }}"
            },
        )
        candidates.addMods()
        candidates.setupLanguageAdapter()
    }

    private fun discoverMods(): Collection<ModCandidate> {
        val discoverer = ModDiscoverer(VersionOverrides(), DependencyOverrides(FabricLoaderImpl.INSTANCE.configDir))
        addCandidateFinderFunction.call(
            discoverer,
            ModContainerModCandidateFinder(service.all() as MutableList<ModContainerImpl>)
        )
        val modsDir = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
        modsDir
            .listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach {
                logger.debug("Discovering mods from {}", it)
                addCandidateFinderFunction.call(
                    discoverer,
                    FilteredDirectoryModCandidateFinder(
                        it,
                        FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment,
                    ),
                )
            }
        return discoverer.discoverMods(FabricLoaderImpl.INSTANCE, envDisabledMods)
    }

    private fun Collection<ModCandidate>.resolveMods(): Collection<ModCandidate> {
        val modId = service.all().asSequence().map { it.metadata.id }.toSet()
        val cacheDir: Path = FabricLoaderImpl.INSTANCE.gameDir.resolve(FabricLoaderImpl.CACHE_DIR_NAME)
        val processedModsDir = cacheDir.resolve("processedMods")
        val candidates =
            ModResolver.resolve(this, FabricLoaderImpl.INSTANCE.environmentType, envDisabledMods)
                .filter { it.id !in modId }
        if (FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment && System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE) != null) {
            RuntimeModRemapper.remap(candidates, cacheDir.resolve("tmp"), processedModsDir)
        }

        // https://github.com/FabricMC/fabric-loader/blob/0.14.22/src/main/java/net/fabricmc/loader/impl/FabricLoaderImpl.java#L267-L277
        for (mod in candidates) {
            if (!mod.hasPath() && !mod.isBuiltin) {
                try {
                    mod.setPaths(listOf<Path>(mod.copyToDir(processedModsDir, false)))
                } catch (e: IOException) {
                    throw RuntimeException("Error extracting mod $mod", e)
                }
            }
        }
        return candidates
    }

    private fun Collection<ModCandidate>.addMods() {
        for (candidate in this) {
            addModFunction.call(FabricLoaderImpl.INSTANCE, candidate)
            candidate.paths.forEach { FabricLauncherBase.getLauncher().addToClassPath(it) }
        }
    }

    private fun Collection<ModCandidate>.setupLanguageAdapter() {
        val adapterMap = adapterMapProperty.call(FabricLoaderImpl.INSTANCE)
        for (candidate in this) {
            val definitions = candidate.metadata.languageAdapterDefinitions
            if (definitions.isEmpty()) continue

            logger.debug("Setting up language adapter for {}", candidate.id)
            definitions.forEach { (id, value) ->
                if (id in adapterMap) throw IllegalArgumentException("Duplicate language adapter ID: $id")
                val adapter = Class.forName(value, true, FabricLauncherBase.getLauncher().targetClassLoader)
                    .kotlin
                    .constructors
                    .firstOrNull { it.parameters.isEmpty() }!!
                    .call() as LanguageAdapter
                adapterMap[id] = adapter
            }
        }
    }
}
