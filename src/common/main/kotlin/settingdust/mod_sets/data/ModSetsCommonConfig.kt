package settingdust.mod_sets.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods.disabledMods
import settingdust.mod_sets.util.LoaderAdapter
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

@Serializable
data class ModSetsCommonConfig(
    val badgeInModMenu: Boolean = true
) {
    companion object : SavingData {
        var instance: ModSetsCommonConfig = ModSetsCommonConfig()
            private set
        private val configPath = LoaderAdapter.configPath / "common.json"

        @OptIn(ExperimentalSerializationApi::class)
        override fun reload() {
            runCatching { LoaderAdapter.configPath.createDirectories() }
            runCatching {
                configPath.createFile()
                configPath.writeText("{}")
            }
            instance = ModSets.configJson.decodeFromStream(configPath.inputStream())
            save()
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun save() {
            ModSets.configJson.encodeToStream(disabledMods, configPath.outputStream())
        }
    }
}
