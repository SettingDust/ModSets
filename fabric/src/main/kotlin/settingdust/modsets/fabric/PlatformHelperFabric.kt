package settingdust.modsets.fabric

import net.fabricmc.loader.api.FabricLoader
import settingdust.modsets.PlatformHelper
import java.nio.file.Path

class PlatformHelperFabric : PlatformHelper {
    override val configDir: Path
        get() = FabricLoader.getInstance().configDir
}
