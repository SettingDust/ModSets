package settingdust.modsets.forge

import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator
import net.minecraftforge.forgespi.locating.IModLocator
import settingdust.modsets.ModSets
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

// Inspired by https://github.com/Chaos02/SubFolderLoader/blob/main/src/main/java/com/chaos02/structuredmodloader/StructuredModLoader.java
class ModSetsModLocator : AbstractJarFileModLocator(), IModLocator {
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
            ModSets.logger.info("Loading mods from {} sub dir in mods", it.size)
            ModSets.logger.debug(it.joinToString())
        }.flatMap {
            it.listDirectoryEntries("*.jar")
        }.stream()
    }
}
