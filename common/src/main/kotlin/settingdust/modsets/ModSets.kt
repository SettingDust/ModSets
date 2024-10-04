package settingdust.modsets

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import org.apache.logging.log4j.LogManager
import settingdust.kinecraft.serialization.ComponentSerializer

object ModSets {
    val ID = "mod_sets"
    val LOGGER = LogManager.getLogger()!!

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true

        serializersModule += SerializersModule {
            contextual(ComponentSerializer)
        }
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
