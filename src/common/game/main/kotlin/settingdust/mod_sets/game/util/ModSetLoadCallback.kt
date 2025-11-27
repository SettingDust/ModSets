package settingdust.mod_sets.game.util

import settingdust.kinecraft.event.Event

object ModSetLoadCallback {
    @JvmField
    val CALLBACK = Event<Callback> { listeners ->
        Callback {
            for (callback in listeners) {
                callback.onModSetLoad()
            }
        }
    }

    fun interface Callback {
        fun onModSetLoad()
    }
}
