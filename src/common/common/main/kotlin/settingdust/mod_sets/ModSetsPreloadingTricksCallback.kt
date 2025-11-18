package settingdust.mod_sets

import org.slf4j.LoggerFactory
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.util.LoaderAdapter
import settingdust.preloading_tricks.api.PreloadingTricksCallback
import settingdust.preloading_tricks.api.PreloadingTricksModCandidatesManager
import settingdust.preloading_tricks.api.PreloadingTricksModManager
import java.nio.file.FileVisitResult
import kotlin.io.path.fileVisitor
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.visitFileTree

class ModSetsPreloadingTricksCallback : PreloadingTricksCallback {
    private val logger = LoggerFactory.getLogger("${ModSets.ID}/SetupMod")
    private val modManager = PreloadingTricksModManager.get<PreloadingTricksModManager<*>>()
    private val candidatesManager = PreloadingTricksModCandidatesManager.get<PreloadingTricksModCandidatesManager>()

    override fun onCollectModCandidates() {
        val subDirectories = buildList {
            LoaderAdapter.modsDir.visitFileTree(fileVisitor {
                onPreVisitDirectory { path, _ ->
                    add(path)
                    FileVisitResult.SKIP_SUBTREE
                }
            }, 1)
        }
        logger.info("Loading mods from {} sub-folders in 'mods' folder", subDirectories.size)
        logger.debug(subDirectories.joinToString { it.fileName.toString() })
        candidatesManager.addAll(subDirectories.flatMap { it.listDirectoryEntries("*.jar") })
    }

    override fun onSetupMods() {
        modManager.removeByIds(ModSetsDisabledMods.disabledMods)
    }
}
