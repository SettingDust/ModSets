package settingdust.modsets

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import kotlin.io.path.*

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Use ModSets.rules instead", ReplaceWith("ModSets.rules"))
object Rules : MutableMap<String, RuleSet> by mutableMapOf() {
    private val configDir = PlatformHelper.configDir / "modsets"

    val modSets = mutableMapOf<String, ModSet>()
    private val _ModSetsRegisterCallback =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val ModSetsRegisterCallback = _ModSetsRegisterCallback.asSharedFlow()

    private val definedModSets = mutableMapOf<String, ModSet>()
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
            if (ModSets.config.common.displayModSetsScreen) {
                builder.category(
                    ConfigCategory.createBuilder().apply {
                        name(Component.translatable("modsets.name"))
                        tooltip(Component.translatable("modsets.description"))
                        groups(
                            modSets.map { modSet ->
                                ListOption.createBuilder<String>()
                                    .apply {
                                        name(modSet.value.text)
                                        modSet.value.description?.let { description(OptionDescription.of(it)) }
                                        initial("")
                                        collapsed(true)
                                        controller { StringControllerBuilder.create(it) }
                                        binding(
                                            Binding.generic(modSet.value.mods.toMutableList(), {
                                                modSet.value.mods.toMutableList()
                                            }) {
                                                modSet.value.mods.clear()
                                                modSet.value.mods.addAll(it)
                                                definedModSets[modSet.key] = modSet.value
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
                        val options = category.groups().flatMap { it.options() }
                        for (option in options) {
                            option.addListener { _, _ ->
                                var needSave = false
                                for (currentOption in options.filter { it != option }) {
                                    if (currentOption.changed()) {
                                        needSave = true
                                        (currentOption as Option<Any>).requestSet(currentOption.binding().value)
                                    }
                                }
                                if (option.changed()) {
                                    (option as Option<Any>).requestSet(option.binding().value)
                                    ModSets.logger.warn("Rule ${option.name()} is conflicting with some other rule. Can't change")
                                }
                                if (needSave) save() // The save won't be called with instant
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
            return builder.save(::save).build()
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
        _ModSetsRegisterCallback.tryEmit(Unit)

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
