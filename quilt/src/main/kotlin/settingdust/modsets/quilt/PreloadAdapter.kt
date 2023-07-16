package settingdust.modsets.quilt

import org.quiltmc.loader.api.LanguageAdapter
import org.quiltmc.loader.api.ModContainer

class PreloadAdapter : LanguageAdapter {
    init {
        ModSetsInjector
    }

    override fun <T : Any?> create(mod: ModContainer?, value: String?, type: Class<T>?): T {
        throw UnsupportedOperationException("This is a hack for pre init before any other mods in quilt")
    }
}
