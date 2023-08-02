package settingdust.modsets.quilt

import org.quiltmc.loader.api.QuiltLoader
import settingdust.modsets.PlatformHelper
import java.nio.file.Path

class PlatformHelperQuilt : PlatformHelper {
    override val configDir: Path
        get() = QuiltLoader.getConfigDir()
}
