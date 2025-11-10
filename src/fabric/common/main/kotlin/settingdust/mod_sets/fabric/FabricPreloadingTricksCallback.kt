package settingdust.mod_sets.fabric

import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.impl.FabricLoaderImpl
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
import net.lenni0451.reflect.Classes
import net.lenni0451.reflect.stream.RStream
import org.slf4j.LoggerFactory
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.fabric.util.FilteredDirectoryModCandidateFinder
import settingdust.mod_sets.fabric.util.ModContainerModCandidateFinder
import settingdust.mod_sets.fabric.util.accessor.FabricLoaderImplAccessor.adapterMap
import settingdust.preloading_tricks.api.PreloadingTricksCallback
import settingdust.preloading_tricks.api.PreloadingTricksModManager
import settingdust.preloading_tricks.fabric.FabricModManager
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.fileVisitor
import kotlin.io.path.visitFileTree

/**
 * Fabric platform preloading tricks callback implementation
 *
 * This class is responsible for setting up and loading additional mods in the Fabric environment,
 * including discovering, resolving, and adding mods to the classpath.
 */
class FabricPreloadingTricksCallback : PreloadingTricksCallback {
    private val logger = LoggerFactory.getLogger("${ModSets.ID}/SetupMod")
    private val service = PreloadingTricksModManager.get<FabricModManager>()
    private var envDisabledMods = hashMapOf<String, Set<ModCandidateImpl>>()

    override fun onSetupMods() {
        val modCandidates = discoverAndResolveMods()
        logModList(modCandidates)
        addModsToClasspath(modCandidates)
        setupLanguageAdapters(modCandidates)
        removeDisabledMods()
    }

    private fun discoverAndResolveMods(): Collection<ModCandidateImpl> {
        return try {
            val discoveredMods = discoverModCandidates()
            resolveModCandidates(discoveredMods)
        } catch (e: ModResolutionException) {
            FabricGuiEntry.displayCriticalError(e, true)
            emptyList()
        }
    }

    private fun discoverModCandidates(): Collection<ModCandidateImpl> {
        val discoverer = createModDiscoverer()
        addExistingModsAsCandidates(discoverer)
        scanModsDirectory(discoverer)
        return discoverer.discoverMods(FabricLoaderImpl.INSTANCE, envDisabledMods)
    }

    private fun createModDiscoverer(): ModDiscoverer {
        return ModDiscoverer(
            VersionOverrides(),
            DependencyOverrides(FabricLoaderImpl.INSTANCE.configDir)
        )
    }

    private fun addExistingModsAsCandidates(discoverer: ModDiscoverer) {
        discoverer.addCandidateFinder(ModContainerModCandidateFinder(service.all()))
    }

    private fun scanModsDirectory(discoverer: ModDiscoverer) {
        val modsDir = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
        val subDirectories = findSubDirectories(modsDir)

        logger.info("Loading mods from {} sub folders in mods", subDirectories.size)
        logger.debug(subDirectories.joinToString { it.fileName.toString() })

        subDirectories.forEach { subDir ->
            discoverer.addCandidateFinder(
                FilteredDirectoryModCandidateFinder(
                    subDir,
                    discoverer,
                    FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment,
                ),
            )
        }
    }

    private fun findSubDirectories(modsDir: Path): List<Path> {
        return buildList {
            modsDir.visitFileTree(fileVisitor {
                onPreVisitDirectory { path, _ ->
                    add(path)
                    FileVisitResult.SKIP_SUBTREE
                }
            }, 1)
        }
    }

    private fun resolveModCandidates(candidates: Collection<ModCandidateImpl>): Collection<ModCandidateImpl> {
        val existingModIds = getExistingModIds()
        val resolvedCandidates = resolveAndFilterMods(candidates, existingModIds)
        handleDevelopmentEnvironmentRemapping(resolvedCandidates)
        extractModsWithoutPaths(resolvedCandidates)
        return resolvedCandidates
    }

    private fun getExistingModIds(): Set<String> {
        return service.all().asSequence().map { it.metadata.id }.toSet()
    }

    private fun resolveAndFilterMods(
        candidates: Collection<ModCandidateImpl>,
        existingModIds: Set<String>
    ): Collection<ModCandidateImpl> {
        return ModResolver.resolve(candidates, FabricLoaderImpl.INSTANCE.environmentType, envDisabledMods)
            .filter { it.id !in existingModIds }
    }

    private fun handleDevelopmentEnvironmentRemapping(candidates: Collection<ModCandidateImpl>) {
        if (FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment &&
            System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE) != null
        ) {
            val cacheDir: Path = FabricLoaderImpl.INSTANCE.gameDir.resolve(FabricLoaderImpl.CACHE_DIR_NAME)
            val processedModsDir = cacheDir.resolve("processedMods")
            RuntimeModRemapper.remap(candidates, cacheDir.resolve("tmp"), processedModsDir)
        }
    }

    private fun extractModsWithoutPaths(candidates: Collection<ModCandidateImpl>) {
        val cacheDir: Path = FabricLoaderImpl.INSTANCE.gameDir.resolve(FabricLoaderImpl.CACHE_DIR_NAME)
        val processedModsDir = cacheDir.resolve("processedMods")

        for (mod in candidates) {
            if (!mod.hasPath() && !mod.isBuiltin) {
                try {
                    mod.paths = listOf(mod.copyToDir(processedModsDir, false))
                } catch (e: IOException) {
                    throw RuntimeException("Error extracting mod $mod", e)
                }
            }
        }
    }

    private fun addModsToClasspath(candidates: Collection<ModCandidateImpl>) {
        candidates.forEach { candidate ->
            service.add(candidate)
            addModPathsToClasspath(candidate)
        }
    }

    private fun addModPathsToClasspath(candidate: ModCandidateImpl) {
        candidate.paths.forEach { path ->
            FabricLauncherBase.getLauncher().addToClassPath(path)
        }
    }

    private fun setupLanguageAdapters(candidates: Collection<ModCandidateImpl>) {
        val adapterMap = FabricLoaderImpl.INSTANCE.adapterMap

        candidates.forEach { candidate ->
            setupLanguageAdapterForMod(candidate, adapterMap)
        }
    }

    private fun setupLanguageAdapterForMod(
        candidate: ModCandidateImpl,
        adapterMap: MutableMap<String, LanguageAdapter>
    ) {
        val definitions = candidate.metadata.languageAdapterDefinitions

        if (definitions.isEmpty()) return

        logger.debug("Setting up language adapter for {}", candidate.id)

        definitions.forEach { (id, value) ->
            registerLanguageAdapter(id, value, adapterMap)
        }
    }

    private fun registerLanguageAdapter(
        id: String,
        className: String,
        adapterMap: MutableMap<String, LanguageAdapter>
    ) {
        if (id in adapterMap) {
            throw IllegalArgumentException("Duplicate language adapter ID: $id")
        }

        val adapter = createLanguageAdapter(className)
        adapterMap[id] = adapter
    }

    private fun createLanguageAdapter(className: String): LanguageAdapter {
        return RStream
            .of(Classes.forName(className, FabricLauncherBase.getLauncher().targetClassLoader))
            .constructors()
            .by()
            .newInstance()
    }

    private fun removeDisabledMods() {
        service.removeByIds(ModSetsDisabledMods.disabledMods)
    }

    private fun logModList(mods: Collection<ModCandidateImpl>) {
        val modListText = buildModListText(mods)
        val modsCount = mods.size

        logger.info(
            "Loading {} additional mod{}:{}",
            modsCount,
            if (modsCount != 1) "s" else "",
            modListText
        )
    }

    private fun buildModListText(mods: Collection<ModCandidateImpl>): StringBuilder {
        val modListText = StringBuilder()
        val lastItemOfNestLevel = BooleanArray(mods.size)
        val topLevelMods = mods.filter { it.parentMods.isEmpty() }
        val topLevelModsCount = topLevelMods.size

        for (i in 0 until topLevelModsCount) {
            val isLastItem = i == topLevelModsCount - 1
            if (isLastItem) lastItemOfNestLevel[0] = true
            appendModToList(topLevelMods[i], modListText, 0, lastItemOfNestLevel)
        }

        return modListText
    }

    private fun appendModToList(
        mod: ModCandidateImpl,
        log: StringBuilder,
        nestLevel: Int,
        lastItemOfNestLevel: BooleanArray
    ) {
        if (log.isNotEmpty()) log.append('\n')

        appendIndentation(log, nestLevel, lastItemOfNestLevel)
        appendModEntryPrefix(log, nestLevel, lastItemOfNestLevel)

        log.append(mod.id)
        log.append(' ')
        log.append(mod.version.friendlyString)

        processNestedMods(mod, log, nestLevel, lastItemOfNestLevel)
    }

    private fun appendIndentation(log: StringBuilder, nestLevel: Int, lastItemOfNestLevel: BooleanArray) {
        for (depth in 0 until nestLevel) {
            log.append(
                when {
                    depth == 0 -> "\t"
                    lastItemOfNestLevel[depth] -> "     "
                    else -> "   | "
                }
            )
        }
    }

    private fun appendModEntryPrefix(log: StringBuilder, nestLevel: Int, lastItemOfNestLevel: BooleanArray) {
        log.append(if (nestLevel == 0) "\t" else "  ")
        log.append(
            when {
                nestLevel == 0 -> "-"
                lastItemOfNestLevel[nestLevel] -> " \\--"
                else -> " |--"
            }
        )
        log.append(' ')
    }

    private fun processNestedMods(
        mod: ModCandidateImpl,
        log: StringBuilder,
        nestLevel: Int,
        lastItemOfNestLevel: BooleanArray
    ) {
        val nestedMods = mod.nestedMods.sortedWith(compareBy { it.metadata.id })

        if (nestedMods.isNotEmpty()) {
            nestedMods.forEachIndexed { index, nestedMod ->
                val isLastItem = index == nestedMods.size - 1
                if (isLastItem) lastItemOfNestLevel[nestLevel + 1] = true
                appendModToList(nestedMod, log, nestLevel + 1, lastItemOfNestLevel)
                if (isLastItem) lastItemOfNestLevel[nestLevel + 1] = false
            }
        }
    }
}
