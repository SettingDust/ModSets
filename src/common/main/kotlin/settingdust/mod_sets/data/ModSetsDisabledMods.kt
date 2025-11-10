package settingdust.mod_sets.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.LoaderAdapter
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

class ModSetsDisabledMods : SavingData {
    companion object {
        var disabledMods = mutableSetOf<String>()
            private set
        lateinit var disabledModsInitial: Set<String>
            private set
        private val configPath = LoaderAdapter.configPath / "disabled_mods.json"

        private fun isDisabledModsInitialInitialized() = ::disabledModsInitial.isInitialized
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun reload() {
        runCatching { LoaderAdapter.configPath.createDirectories() }
        runCatching {
            configPath.createFile()
            configPath.writeText("[]")
        }
        disabledMods = ModSets.configJson.decodeFromStream(configPath.inputStream())
        if (!isDisabledModsInitialInitialized()) disabledModsInitial = disabledMods
        save()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun save() {
        ModSets.configJson.encodeToStream(disabledMods, configPath.outputStream())
    }
}
