package settingdust.modsets

import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager

object ModSets {
    val ID = "mod_sets"
    val LOGGER = LogManager.getLogger()!!

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
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
