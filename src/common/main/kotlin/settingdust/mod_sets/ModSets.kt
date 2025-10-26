package settingdust.mod_sets

import org.apache.logging.log4j.LogManager
import settingdust.mod_sets.util.MinecraftAdapter
import settingdust.mod_sets.util.ServiceLoaderUtil

object ModSets {
    const val ID = "mod_sets"

    val LOGGER = LogManager.getLogger()

    init {
        ServiceLoaderUtil.defaultLogger = LOGGER
    }

    fun id(path: String) = MinecraftAdapter.id(ID, path)
}