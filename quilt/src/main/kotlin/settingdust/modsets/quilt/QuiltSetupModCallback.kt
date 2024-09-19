package settingdust.modsets.quilt

import org.quiltmc.loader.api.ModContainer
import settingdust.modsets.ModSetsConfig
import settingdust.preloadingtricks.SetupModCallback
import settingdust.preloadingtricks.quilt.QuiltModSetupService

class QuiltSetupModCallback : SetupModCallback {
    init {
        val service = QuiltModSetupService.INSTANCE
        service.removeIf { (it as ModContainer).metadata().id() in ModSetsConfig.disabledMods }
    }
}
