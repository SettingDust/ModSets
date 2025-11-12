package settingdust.mod_sets.fabric.util

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.FabricLoaderImpl
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.LoaderAdapter
import java.nio.file.Path
import kotlin.io.path.div

class LoaderAdapter : LoaderAdapter {
    override val isClient = FabricLoader.getInstance().environmentType === EnvType.CLIENT

    override fun isModLoaded(modId: String) = FabricLoader.getInstance().isModLoaded(modId)

    override val configPath by lazy { FabricLoader.getInstance().configDir / ModSets.ID }

    override val modsDir: Path = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
}
