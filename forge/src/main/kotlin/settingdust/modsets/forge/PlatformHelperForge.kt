package settingdust.modsets.forge

import net.minecraftforge.fml.loading.FMLPaths
import settingdust.modsets.PlatformHelper
import java.nio.file.Path

class PlatformHelperForge : PlatformHelper {
    override val configDir: Path
        get() = FMLPaths.CONFIGDIR.get()
}
