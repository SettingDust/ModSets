package settingdust.modsets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Suppress("DEPRECATION", "UnusedReceiverParameter")
val ModSets.config: ModSetsConfig
    get() = ModSetsConfig

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.config instead", ReplaceWith("ModSets.config"))
object ModSetsConfig {
    val disabledMods = mutableSetOf<String>()
    var common = CommonConfig()
        private set

    private val configDir = PlatformHelper.configDir / "modsets"
    private val commonConfigPath = configDir / "common.json"
    private val disabledModsPath = configDir / "disabled_mods.json"

    private val json = Json {
        encodeDefaults = true
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        load()
    }

    fun load() {
        try {
            configDir.createDirectories()
        } catch (_: Exception) {}

        try {
            disabledModsPath.createFile()
        } catch (_: Exception) {}

        try {
            commonConfigPath.createFile()
        } catch (_: Exception) {}

        try {
            disabledMods.clear()
            disabledMods.addAll(json.decodeFromStream(disabledModsPath.inputStream()))
        } catch (_: Exception) {}

        try {
            common = json.decodeFromStream(commonConfigPath.inputStream())
        } catch (_: Exception) {}
        save()
    }

    fun save() {
        commonConfigPath.outputStream().use { json.encodeToStream(common, it) }
        disabledModsPath.outputStream().use {
            json.encodeToStream(
                disabledMods,
                it,
            )
        }
    }
}

@Serializable
data class CommonConfig(
    val displayModSetsScreen: Boolean = true, /*val nestedInModMenu: Boolean = true,*/
)
