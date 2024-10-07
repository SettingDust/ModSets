package settingdust.modsets.quilt

import org.quiltmc.loader.api.QuiltLoader
import settingdust.modsets.PlatformHelper
import kotlin.io.path.div

class PlatformHelperQuilt : PlatformHelper {
    override val configDir by lazy {
        QuiltLoader.getConfigDir() / "modsets"
    }
}
