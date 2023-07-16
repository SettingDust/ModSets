package settingdust.modsets

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.*

@Suppress("DEPRECATION")
val ModSets.config: ModSetsConfig
    get() = ModSetsConfig

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.config instead", ReplaceWith("ModSets.config"))
object ModSetsConfig {
    val disabledMods = mutableSetOf<String>()

    private val configDir = FabricLoader.getInstance().configDir / "modsets"
    private val disabledModsPath = configDir / "disabled_mods.json"

    private val json = Json {
        isLenient = true
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
