package settingdust.modsets

import dev.isxander.yacl.api.ConfigCategory
import dev.isxander.yacl.api.YetAnotherConfigLib
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import settingdust.kinecraft.serialization.ComponentSerializer
import settingdust.kinecraft.serialization.GsonElementSerializer
import kotlin.io.path.*

@Suppress("DEPRECATION")
val ModSets.config: ModSetsConfig
    get() = ModSetsConfig

@Suppress("DEPRECATION")
val ModSets.rules: Rules
    get() = Rules

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.rules instead", ReplaceWith("ModSets.rules"))
data object Rules : MutableMap<String, RuleSet> by mutableMapOf() {
    private val configDir = FabricLoader.getInstance().configDir / "modsets"

    val modSets = mutableMapOf<String, ModSet>()

    private val userModSets = mutableMapOf<String, ModSet>()
    private val modSetsPath = configDir / "modsets.json"

    private val rulesDir = configDir / "rules"

    private val json = Json {
        isLenient = true
        serializersModule = SerializersModule {
            contextual(ComponentSerializer)
            contextual(GsonElementSerializer)
        }
    }

    private val config: YetAnotherConfigLib
        get() {
            load()
            val builder = YetAnotherConfigLib.createBuilder().title(Component.translatable("modsets.name"))
            if (this@Rules.isNotEmpty()) {
                builder.run {
                    categories(
                        this@Rules.map { (_, ruleSet) ->
                            ConfigCategory.createBuilder().run {
                                name(ruleSet.text)
                                ruleSet.tooltip?.let { tooltip(it) }
                                ruleSet.rules.forEach { rule ->
                                    when (val controller = rule.controller) {
                                        is OptionRule<*> -> option(controller.get(rule))
                                        is GroupRule -> group(controller.get(rule))
                                    }
                                }
                                build()
                            }
                        },
                    )
                    save(ModSets.config::save)
                    build()
                }
            } else {
                builder.category(
                    ConfigCategory.createBuilder().name(Component.translatable("modsets.no_rules")).build(),
                )
            }
            return builder.save(ModSets.config::save).build()
        }

    init {
        load()
        modSets.putAll(userModSets)
    }

    private fun load() {
        try {
            configDir.createDirectories()
            modSetsPath.createFile()
            modSetsPath.writeText("{}")
        } catch (_: Exception) {
        }

        userModSets.clear()
        userModSets.putAll(json.decodeFromStream(modSetsPath.inputStream()))

        clear()
        rulesDir.listDirectoryEntries("*.json").forEach {
            try {
                this[it.nameWithoutExtension] = json.decodeFromStream(it.inputStream())
            } catch (e: Exception) {
                throw RuntimeException("Failed to load rule ${it.name}", e)
            }
        }
    }

    internal fun createScreen(parent: Screen) = config.generateScreen(parent)
}

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.config instead", ReplaceWith("ModSets.config"))
data object ModSetsConfig {
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
