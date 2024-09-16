package settingdust.modsets.fabric

import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.ModContainerImpl
import net.fabricmc.loader.impl.discovery.ModCandidateImpl
import net.fabricmc.loader.impl.discovery.ModDiscoverer
import net.fabricmc.loader.impl.discovery.ModResolutionException
import net.fabricmc.loader.impl.discovery.ModResolver
import net.fabricmc.loader.impl.discovery.RuntimeModRemapper
import net.fabricmc.loader.impl.gui.FabricGuiEntry
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.metadata.DependencyOverrides
import net.fabricmc.loader.impl.metadata.VersionOverrides
import net.fabricmc.loader.impl.util.SystemProperties
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import org.slf4j.LoggerFactory
import settingdust.modsets.ModSets
import settingdust.modsets.config
import settingdust.preloadingtricks.SetupModCallback
import settingdust.preloadingtricks.fabric.FabricModSetupService
import java.io.IOException
import java.nio.file.Path
import java.util.stream.Collectors
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
    private var envDisabledMods = mutableMapOf<String, Set<ModCandidateImpl>>()

    @Suppress("UNCHECKED_CAST")
    private val adapterMapProperty =
        FabricLoaderImpl::class.memberProperties.single { it.name == "adapterMap" } as KProperty<MutableMap<String, LanguageAdapter>>

    @Suppress("UNCHECKED_CAST")
    private val addCandidateFinderFunction =
        ModDiscoverer::class.functions.single { it.name == "addCandidateFinder" } as KFunction<Void>


    init {
        adapterMapProperty.isAccessible = true

        val candidates = try {
            discoverMods().resolveMods()
        } catch (e: ModResolutionException) {
            FabricGuiEntry.displayCriticalError(e, true)
            emptyList() // unreachable
        }
        candidates.dumpModList()
        candidates.addMods()
        candidates.setupLanguageAdapter()

        service.removeIf { it.metadata.id in ModSets.config.disabledMods }
    }

    private fun discoverMods(): Collection<ModCandidateImpl> {
        val discoverer = ModDiscoverer(VersionOverrides(), DependencyOverrides(FabricLoaderImpl.INSTANCE.configDir))
        addCandidateFinderFunction.call(
            discoverer,
            ModContainerModCandidateFinder(service.all() as MutableList<ModContainerImpl>)
        )
        val modsDir = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
        val subDirs = modsDir
            .listDirectoryEntries()
            .filter { it.isDirectory() }

        logger.info("Loading mods from {} sub folders in mods", subDirs.size)
        logger.debug(subDirs.joinToString { it.fileName.toString() })

        subDirs
            .forEach {
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

    private fun Collection<ModCandidateImpl>.resolveMods(): Collection<ModCandidateImpl> {
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

    private fun Collection<ModCandidateImpl>.addMods() {
        for (candidate in this) {
            service.add(candidate)
            candidate.paths.forEach { FabricLauncherBase.getLauncher().addToClassPath(it) }
        }
    }

    private fun Collection<ModCandidateImpl>.setupLanguageAdapter() {
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

    private fun Collection<ModCandidateImpl>.dumpModList() {
        val modListText = StringBuilder()
        val lastItemOfNestLevel = BooleanArray(size)
        val topLevelMods = stream()
            .filter { mod: ModCandidateImpl -> mod.parentMods.isEmpty() }
            .collect(Collectors.toList())
        val topLevelModsCount = topLevelMods.size
        for (i in 0 until topLevelModsCount) {
            val lastItem = i == topLevelModsCount - 1
            if (lastItem) lastItemOfNestLevel[0] = true
            topLevelMods[i].dumpModList0(modListText, 0, lastItemOfNestLevel)
        }
        val modsCount = size
        Log.info(
            LogCategory.GENERAL,
            "Loading %d additional mod%s:%n%s",
            modsCount,
            if (modsCount != 1) "s" else "",
            modListText
        )
    }

    private fun ModCandidateImpl.dumpModList0(log: StringBuilder, nestLevel: Int, lastItemOfNestLevel: BooleanArray) {
        if (log.isNotEmpty()) log.append('\n')
        for (depth in 0 until nestLevel) {
            log.append(if (depth == 0) "\t" else if (lastItemOfNestLevel[depth]) "     " else "   | ")
        }
        log.append(if (nestLevel == 0) "\t" else "  ")
        log.append(if (nestLevel == 0) "-" else if (lastItemOfNestLevel[nestLevel]) " \\--" else " |--")
        log.append(' ')
        log.append(id)
        log.append(' ')
        log.append(version.friendlyString)
        val nestedMods: List<ModCandidateImpl> = ArrayList(nestedMods)
        nestedMods.sortedBy { it.metadata.id }
        if (nestedMods.isNotEmpty()) {
            val iterator = nestedMods.iterator()
            var nestedMod: ModCandidateImpl
            var lastItem: Boolean
            while (iterator.hasNext()) {
                nestedMod = iterator.next()
                lastItem = !iterator.hasNext()
                if (lastItem) lastItemOfNestLevel[nestLevel + 1] = true
                nestedMod.dumpModList0(log, nestLevel + 1, lastItemOfNestLevel)
                if (lastItem) lastItemOfNestLevel[nestLevel + 1] = false
            }
        }
    }
}
