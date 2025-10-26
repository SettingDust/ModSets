package settingdust.mod_sets.fabric

import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.Entrypoint

object ModSetsFabric {
    init {
        requireNotNull(ModSets)
        Entrypoint.construct()
    }

    fun init() {
        Entrypoint.init()
    }

    fun clientInit() {
        Entrypoint.clientInit()
    }
}
