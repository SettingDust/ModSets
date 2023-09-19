package settingdust.modsets.game

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import kotlinx.coroutines.runBlocking
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
import settingdust.modsets.ModSets
import settingdust.modsets.PlatformHelper
import settingdust.modsets.config
import kotlin.io.path.*

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.rules instead", ReplaceWith("ModSets.rules"))
object Rules : MutableMap<String, RuleSet> by hashMapOf() {
    private val configDir = PlatformHelper.configDir / "modsets"

    val modSets = hashMapOf<String, ModSet>()
    val ModSetsRegisterCallbacks = mutableSetOf<() -> Unit>()

    private val definedModSets = hashMapOf<String, ModSet>()
    private val modSetsPath = configDir / "modsets.json"

    private val rulesDir = configDir / "rules"

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(ComponentSerializer)
            contextual(GsonElementSerializer)
        }
        prettyPrint = true
    }

    fun MutableMap<String, ModSet>.getOrThrow(name: String) = requireNotNull(get(name)) { "Mod sets $name not exist" }

    private val config: YetAnotherConfigLib
        get() {
            load()
            val builder = YetAnotherConfigLib.createBuilder().title(Component.translatable("modsets.name"))
            if (ModSets.config.common.displayModSetsScreen && modSets.isNotEmpty()) {
                builder.category(
                    ConfigCategory.createBuilder().apply {
                        name(Component.translatable("modsets.name"))
                        tooltip(Component.translatable("modsets.description"))
                        groups(
                            modSets.map { (name, modSet) ->
                                ListOption.createBuilder<String>()
                                    .apply {
                                        name(modSet.text)
                                        modSet.description?.let { description(OptionDescription.of(it)) }
                                        initial("")
                                        collapsed(true)
                                        controller { StringControllerBuilder.create(it) }
                                        binding(
                                            Binding.generic(modSet.mods.toMutableList(), {
                                                modSet.mods.toMutableList()
                                            }) {
                                                modSet.mods.clear()
                                                modSet.mods.addAll(it)
                                                definedModSets[name] = modSet
                                            },
                                        )
                                    }
                                    .build()
                            },
                        )
                    }.build(),
                )
            }
            if (this@Rules.isNotEmpty()) {
                builder.categories(
                    this@Rules.map { (_, ruleSet) ->
                        val category = ConfigCategory.createBuilder().apply {
                            name(ruleSet.text)
                            ruleSet.description?.let { tooltip(it) }
                            ruleSet.rules.forEach { rule ->
                                when (val controller = rule.controller) {
                                    is OptionRule<*> -> option(controller.get(rule))
                                    is GroupRule -> group(controller.get(rule))
                                }
                            }
                        }.build()
                        // Since the options are instant and may be affected by the others. Update the changed options to correct value
                        val options = category.groups().flatMap { it.options() as Iterable<Option<Any>> }
                        for (option in options) {
                            option.addListener { _, _ ->
                                var changed = false
                                for (it in options.filter { it != option && it.changed() }) {
                                    it.requestSet(it.binding().value)
                                    if (!changed && option.changed()) {
                                        ModSets.logger.warn("Option ${option.name()} is conflicting with ${it.name()}. Can't change")
                                        changed = true
                                    }
                                }
                                if (option.changed()) {
                                    ModSets.logger.warn("Option ${option.name()} is conflicting with unknown option. Can't change")
                                    option.requestSet(option.binding().value)
                                }
                                save() // The save won't be called with instant
                            }
                        }
                        category
                    },
                )
            } else {
                builder.category(
                    ConfigCategory.createBuilder().name(Component.translatable("modsets.no_rules")).build(),
                )
            }
            return builder.save(ModSets.rules::save).build()
        }

    init {
        load()
    }

    private fun load() {
        ModSets.config.load()
        try {
            configDir.createDirectories()
            rulesDir.createDirectories()
            modSetsPath.createFile()
            modSetsPath.writeText("{}")
        } catch (_: Exception) {
        }

        definedModSets.clear()
        modSets.clear()
        modSetsPath.inputStream().use {
            definedModSets.putAll(json.decodeFromStream(it))
        }
        modSets.putAll(definedModSets)
        runBlocking { ModSetsRegisterCallbacks.forEach { it() } }

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

    fun createScreen(parent: Screen) = config.generateScreen(parent)
}

@Suppress("DEPRECATION", "UnusedReceiverParameter")
val ModSets.rules: Rules
    get() = Rules
