package settingdust.modsets

import org.apache.logging.log4j.LogManager
import org.quiltmc.qkl.library.serialization.CodecFactory

object ModSets {
    val LOGGER = LogManager.getLogger()!!

    val CODEC_FACTORY = CodecFactory {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    init {
        reload()
    }

    fun reload() {
        ModSetsConfig.reload()
    }

    fun save() {
        ModSetsConfig.save()
    }
}
