package settingdust.mod_sets.neoforge.util

import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.fml.loading.LoadingModList
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.LoaderAdapter
import java.nio.file.Path
import kotlin.io.path.div

class LoaderAdapter : LoaderAdapter {
    override val isClient: Boolean
        get() = FMLLoader.getDist().isClient

    override fun isModLoaded(modId: String) = LoadingModList.get().getModFileById(modId) != null

    override val configPath by lazy { FMLPaths.CONFIGDIR.get() / ModSets.ID }

    override val modsDir: Path = FMLPaths.MODSDIR.get()
}
