package settingdust.modsets.quilt

import org.quiltmc.loader.api.ModContainer
import settingdust.modsets.ModSets
import settingdust.modsets.config
import settingdust.preloadingtricks.SetupModCallback
import settingdust.preloadingtricks.quilt.QuiltLanguageProviderCallback

class QuiltSetupModCallback : SetupModCallback {
    init {
        val service = QuiltLanguageProviderCallback.QuiltModSetupService.INSTANCE
        service.removeIf { (it as ModContainer).metadata().id() in ModSets.config.disabledMods }
    }
}
