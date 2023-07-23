package settingdust.modsets

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import settingdust.kinecraft.serialization.ComponentSerializer
import settingdust.kinecraft.serialization.GsonElementSerializer
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.rules instead", ReplaceWith("ModSets.rules"))
object Rules : MutableMap<String, RuleSet> by mutableMapOf() {
    private val configDir = PlatformHelper.configDir / "modsets"

    val modSets = mutableMapOf<String, ModSet>()

    private val definedModSets = mutableMapOf<String, ModSet>()
    private val modSetsPath = configDir / "modsets.json"

    private val rulesDir = configDir / "rules"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(ComponentSerializer)
            contextual(GsonElementSerializer)
        }
        prettyPrint = true
    }

    private val config: YetAnotherConfigLib
        get() {
            load()
            val builder = YetAnotherConfigLib.createBuilder().title(Component.translatable("modsets.name"))
            builder.category(
                ConfigCategory.createBuilder().apply {
                    name(Component.translatable("modsets.name"))
                    tooltip(Component.translatable("modsets.description"))
                    for (modSet in modSets) {
                        option(
                            ListOption.createBuilder<String>()
                                .apply {
                                    name(modSet.value.text)
                                    modSet.value.description?.let { description(OptionDescription.of(it)) }
                                    initial("")
                                    collapsed(true)
                                    controller { StringControllerBuilder.create(it) }
                                    binding(
                                        Binding.generic(modSet.value.mods, {
                                            modSet.value.mods
                                        }) {
                                            modSet.value.mods.clear()
                                            modSet.value.mods.addAll(it)
                                            definedModSets[modSet.key] = modSet.value
                                        },
                                    )
                                }
                                .build(),
                        )
                    }
                }.build(),
            )
            if (this@Rules.isNotEmpty()) {
                builder.categories(
                    this@Rules.map { (_, ruleSet) ->
                        ConfigCategory.createBuilder().apply {
                            name(ruleSet.text)
                            ruleSet.description?.let { tooltip(it) }
                            ruleSet.rules.forEach { rule ->
                                when (val controller = rule.controller) {
                                    is OptionRule<*> -> option(controller.get(rule))
                                    is GroupRule -> group(controller.get(rule))
                                }
                            }
                        }.build()
                    },
                )
            } else {
                builder.category(
                    ConfigCategory.createBuilder().name(Component.translatable("modsets.no_rules")).build(),
                )
            }
            return builder.save(::save).build()
        }

    init {
        load()
        modSets.putAll(definedModSets)
    }

    private fun load() {
        try {
            configDir.createDirectories()
            rulesDir.createDirectories()
            modSetsPath.createFile()
            modSetsPath.writeText("{}")
        } catch (_: Exception) {
        }

        definedModSets.clear()
        modSetsPath.inputStream().use {
            definedModSets.putAll(json.decodeFromStream(it))
        }

        clear()
        rulesDir.listDirectoryEntries("*.json").forEach {
            try {
                it.inputStream().use { stream ->
                    this[it.nameWithoutExtension] = json.decodeFromStream(stream)
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to load rule ${it.name}", e)
            }
        }
    }

    private fun save() {
        ModSets.config.save()
        modSetsPath.outputStream().use {
            json.encodeToStream(definedModSets, it)
        }
    }

    internal fun createScreen(parent: Screen) = config.generateScreen(parent)
}

@Suppress("DEPRECATION")
val ModSets.rules: Rules
    get() = Rules
