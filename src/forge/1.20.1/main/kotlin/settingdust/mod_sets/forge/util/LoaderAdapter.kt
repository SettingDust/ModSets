package settingdust.mod_sets.forge.util

import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.fml.loading.LoadingModList
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.LoaderAdapter
import kotlin.io.path.div

class LoaderAdapter : LoaderAdapter {
    override val isClient: Boolean
        get() = FMLLoader.getDist().isClient

    override fun isModLoaded(modId: String) = LoadingModList.get().getModFileById(modId) != null

    override val configPath by lazy { FMLPaths.CONFIGDIR.get() / ModSets.ID }
}
