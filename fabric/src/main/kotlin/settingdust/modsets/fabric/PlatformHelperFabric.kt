package settingdust.modsets.fabric

import net.fabricmc.loader.api.FabricLoader
import settingdust.modsets.PlatformHelper
import kotlin.io.path.div

class PlatformHelperFabric : PlatformHelper {
    override val configDir by lazy {
        FabricLoader.getInstance().configDir / "modsets"
    }
}
