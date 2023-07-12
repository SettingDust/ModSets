package settingdust.modsets

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.loader.impl.discovery.*
import net.fabricmc.loader.impl.gui.FabricGuiEntry
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.metadata.DependencyOverrides
import net.fabricmc.loader.impl.metadata.VersionOverrides
import net.fabricmc.loader.impl.util.SystemProperties
import net.fabricmc.loader.impl.util.log.Log
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ModSetsInjector {
    private val loader = FabricLoader.getInstance() as FabricLoaderImpl
    private val mods = loader.modsInternal
    private var envDisabledMods = mutableMapOf<String, Set<ModCandidate>>()

    @Suppress("UNCHECKED_CAST")
    private val modsProperty =
        FabricLoaderImpl::class.memberProperties.single { it.name == "mods" } as KMutableProperty<List<ModContainerImpl>>

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
        try {
            requireNotNull(ModSets.config)
        } catch (e: Exception) {
            Log.error(ModSets.logCategory, "ModSets config loading failed", e)
        }
        modsProperty.isAccessible = true
        adapterMapProperty.isAccessible = true
        addModFunction.isAccessible = true
        hookSetupMods()
    }

    private data object DummyList : MutableList<ModContainerImpl> by mods {
        override fun iterator(): MutableIterator<ModContainerImpl> {
            setupModsInvoked()
            return mods.iterator()
        }
    }

    private fun hookSetupMods() {
        modsProperty.setter.call(loader, DummyList)
    }

    private fun setupModsInvoked() {
        modsProperty.setter.call(loader, mods)

        val candidates = try {
            discoverMods().resolveMods()
        } catch (e: ModResolutionException) {
            FabricGuiEntry.displayCriticalError(e, true)
            emptyList() // unreachable
        }
        Log.info(
            ModSets.logCategory,
            "Loading %s additional mod%s%s",
            candidates.size,
            if (candidates.size > 1) "s" else "",
            if (candidates.isEmpty()) {
                ""
            } else {
                ":\n ${candidates.joinToString("\n") { "\t- ${it.id}@${it.version.friendlyString}" }}"
            },
        )
        candidates.addMods()
        candidates.setupLanguageAdapter()
    }

    private fun discoverMods(): Collection<ModCandidate> {
        val discoverer = ModDiscoverer(VersionOverrides(), DependencyOverrides(loader.configDir))
        addCandidateFinderFunction.call(discoverer, ModContainerModCandidateFinder(mods))
        val modsDir = loader.modsDirectory.toPath()
        modsDir
            .listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach {
                Log.debug(ModSets.logCategory, "Discovering mods from %s", it)
                addCandidateFinderFunction.call(
                    discoverer,
                    FilteredDirectoryModCandidateFinder(
                        it,
                        loader.isDevelopmentEnvironment,
                    ),
                )
            }
        return discoverer.discoverMods(loader, envDisabledMods)
    }

    private fun Collection<ModCandidate>.resolveMods(): Collection<ModCandidate> {
        val modId = mods.asSequence().map { it.metadata.id }.toSet()
        val candidates =
            ModResolver.resolve(this, loader.environmentType, envDisabledMods)
                .filter { it.id !in modId }
        if (loader.isDevelopmentEnvironment && System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE) != null) {
            val cacheDir: Path = loader.gameDir.resolve(FabricLoaderImpl.CACHE_DIR_NAME)
            RuntimeModRemapper.remap(candidates, cacheDir.resolve("tmp"), cacheDir.resolve("processedMods"))
        }
        return candidates
    }

    private fun Collection<ModCandidate>.addMods() {
        for (candidate in this) {
            addModFunction.call(loader, candidate)
            candidate.paths.forEach { FabricLauncherBase.getLauncher().addToClassPath(it) }
        }
    }

    private fun Collection<ModCandidate>.setupLanguageAdapter() {
        val adapterMap = adapterMapProperty.call(loader)
        for (candidate in this) {
            val definitions = candidate.metadata.languageAdapterDefinitions
            if (definitions.isEmpty()) continue

            Log.debug(ModSets.logCategory, "Setting up language adapter for %s", candidate.id)
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
