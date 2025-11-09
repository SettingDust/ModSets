package settingdust.mod_sets

import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import settingdust.mod_sets.data.SavingData
import settingdust.mod_sets.util.MinecraftAdapter

object ModSets {
    const val ID = "mod_sets"

    val LOGGER = LogManager.getLogger()

    val configJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        reload()
    }

    fun id(path: String) = MinecraftAdapter.id(ID, path)

    fun reload() {
        for (clazz in SavingData::class.sealedSubclasses) {
            clazz.objectInstance?.reload()
        }
    }

    fun save() {
        for (clazz in SavingData::class.sealedSubclasses) {
            clazz.objectInstance?.save()
        }
    }
}
