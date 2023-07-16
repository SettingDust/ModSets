package settingdust.modsets.fabric

import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.api.ModContainer

class PreloadAdapter : LanguageAdapter {
    init {
        ModSetsInjector
    }

    override fun <T : Any?> create(mod: ModContainer?, value: String?, type: Class<T>?): T {
        throw UnsupportedOperationException("This is a hack for pre init before any other mods in fabric")
    }
}
