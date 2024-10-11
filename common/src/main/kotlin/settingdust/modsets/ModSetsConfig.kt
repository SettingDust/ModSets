package settingdust.modsets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object ModSetsConfig {
    @Serializable
    data class Common(val badgeInModMenu: Boolean = true)

    private val commonPath = PlatformHelper.configDir / "common.json"
    var common = Common()
        private set

    private val disabledModsPath = PlatformHelper.configDir / "disabled_mods.json"
    var disabledMods: MutableSet<String> = mutableSetOf()
        private set

    init {
        reload()
    }

    fun reload() {
        runCatching { PlatformHelper.configDir.createDirectories() }

        runCatching { disabledModsPath.createFile() }
        runCatching {
            disabledMods = ModSets.json.decodeFromStream(disabledModsPath.inputStream())
        }
        runCatching { commonPath.createFile() }
        runCatching {
            common =
                ModSets.json.decodeFromStream(commonPath.inputStream())
        }
        save()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save() {
        ModSets.json.encodeToStream(common, commonPath.outputStream())
        ModSets.json.encodeToStream(disabledMods, disabledModsPath.outputStream())
    }
}
