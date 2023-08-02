package settingdust.modsets.forge

import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator
import net.minecraftforge.forgespi.locating.IModLocator
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

// Inspired by https://github.com/Chaos02/SubFolderLoader/blob/main/src/main/java/com/chaos02/structuredmodloader/StructuredModLoader.java
class DummyModSetsModLocator : AbstractJarFileModLocator(), IModLocator {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        val directoryModSet = mutableMapOf<String, MutableList<String>>()
    }

    private val mods = mutableListOf<Path>()

    override fun name() = "mod sets"

    override fun initArguments(arguments: MutableMap<String, *>?) {
    }

    override fun createMod(vararg path: Path?): IModLocator.ModFileOrException {
        val result = super.createMod(*path)
        if (result.file == null) return result
        val dirName = result.file.filePath.parent.name
        directoryModSet.getOrPut(dirName) { mutableListOf() } += result.file.modInfos.map { it.modId }
        return result
    }

    override fun scanCandidates(): Stream<Path> {
        val modsDir = FMLPaths.GAMEDIR.get() / FMLPaths.MODSDIR.get()
        return modsDir.listDirectoryEntries().filter { it.isDirectory() }.also {
            logger.info("Loading mods from {} sub dir in mods", it.size)
            logger.debug(it.joinToString())
        }.flatMap {
            it.listDirectoryEntries("*.jar")
        }.stream()
    }
}
