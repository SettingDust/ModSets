package settingdust.modsets

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import dev.isxander.yacl.api.YetAnotherConfigLib
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.io.path.*

@Suppress("DEPRECATION")
val ModSets.config: ModSetsConfig
    get() = ModSetsConfig

@Suppress("DEPRECATION")
val ModSets.rules: Rules
    get() = Rules

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.rules: instead", ReplaceWith("ModSets.rules"))
data object Rules : MutableMap<String, RuleSet> by mutableMapOf() {
    private val configDir = FabricLoader.getInstance().configDir / "modsets"

    val modSets = mutableMapOf<String, ModSet>()

    private val userModSets = mutableMapOf<String, ModSet>()
    private val modSetsPath = configDir / "modsets.json"

    private val rulesDir = configDir / "rules"

    private val config: YetAnotherConfigLib
        get() {
            load()
            val builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("modsets.name"))

            return builder.save(ModSets.config::save).build()
        }

    init {
        load()
        modSets.putAll(userModSets)
    }

    private fun load() {
        try {
            configDir.createDirectories()
            (configDir / "modsets.json").createFile()
        } catch (_: Exception) {
        }

        userModSets.clear()
        userModSets.putAll(
            ModSets.config.hocon.decodeFromConfig(
                ConfigFactory.parseReader(modSetsPath.reader()),
            ),
        )

        rulesDir.listDirectoryEntries("*.json").forEach {
            try {
                clear()
                this[it.nameWithoutExtension] = ModSets.config.hocon.decodeFromConfig(
                    ConfigFactory.parseReader(it.reader()),
                )
            } catch (_: Exception) {
            }
        }
    }

    internal fun createScreen(parent: Screen) = config.generateScreen(parent)
}

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.config: instead", ReplaceWith("ModSets.config"))
data object ModSetsConfig {
    val disabledMods = mutableSetOf<String>()

    private val configDir = FabricLoader.getInstance().configDir / "modsets"
    private val disabledModsPath = configDir / "disabled_mods.json"

    internal val hocon = Hocon { }

    init {
        load()
    }

    private fun load() {
        try {
            configDir.createDirectories()
            (configDir / "disabled_mods.json").createFile()
            save()
        } catch (_: Exception) {
        }

        try {
            disabledMods.clear()
            disabledMods.addAll(
                hocon.decodeFromConfig(
                    ConfigFactory.parseReader(disabledModsPath.reader()),
                ),
            )
        } catch (_: Exception) {
        }
    }

    internal fun save() {
        disabledModsPath.writeText(
            hocon.encodeToConfig(disabledMods).root().render(
                ConfigRenderOptions.defaults().apply {
                    json = false
                },
            ),
        )
    }
}
