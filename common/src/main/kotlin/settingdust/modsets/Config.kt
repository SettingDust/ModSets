package settingdust.modsets

import kotlinx.serialization.ExperimentalSerializationApi
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

    private val configDir = PlatformHelper.configDir / "modsets"
    private val disabledModsPath = configDir / "disabled_mods.json"

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        load()
    }

    private fun load() {
        try {
            configDir.createDirectories()
            disabledModsPath.createFile()
            save()
        } catch (_: Exception) {
        }

        try {
            disabledMods.clear()
            disabledMods.addAll(json.decodeFromStream(disabledModsPath.inputStream()))
        } catch (_: Exception) {
        }
    }

    internal fun save() {
        disabledModsPath.outputStream().use {
            json.encodeToStream(
                disabledMods,
                it,
            )
        }
    }
}
