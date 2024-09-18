package settingdust.modsets.ingame

import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.dsl.YetAnotherConfigLib
import dev.isxander.yacl3.dsl.onReady
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.GsonHelper
import org.quiltmc.qkl.library.serialization.annotation.CodecSerializable
import settingdust.kinecraft.serialization.unwrap
import settingdust.modsets.ModSets
import settingdust.modsets.ModSetsConfig
import settingdust.modsets.PlatformHelper
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.reader
import kotlin.io.path.writeText

@CodecSerializable
data class ModSet(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val mods: MutableSet<String>,
) {
    companion object {
        val CODEC = ModSets.CODEC_FACTORY.create<ModSet>()
        val MAP_CODEC: Codec<MutableMap<String, ModSet>> =
            Codec.unboundedMap(Codec.STRING, CODEC)
    }
}

object ModSetsIngameConfig {
    private val modSetsPath = PlatformHelper.configDir / "modsets.json"
    var modSets: MutableMap<String, ModSet> = mutableMapOf()
        private set
    val modIdToModSets = mutableMapOf<String, Set<String>>()
    private val definedModSets = mutableMapOf<String, ModSet>()
    val MOD_SET_REGISTER_CALLBACK = WaitingSharedFlow<Unit>()

    private val rulesDir = PlatformHelper.configDir / "rules"
    var rules: MutableMap<String, RuleSet> = mutableMapOf()
        private set

    fun MutableMap<String, ModSet>.getOrThrow(name: String) =
        requireNotNull(get(name)) { "Mod sets $name not exist" }

    suspend fun reload() {
        runCatching {
            modSetsPath.createFile()
            modSetsPath.writeText("{}")
        }

        runCatching {
            definedModSets.clear()
            definedModSets.putAll(
                ModSet.MAP_CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(modSetsPath.reader()))
                    .unwrap()
            )
        }
        modSets.clear()
        modSets.putAll(definedModSets)

        MOD_SET_REGISTER_CALLBACK.emit(Unit)

        modIdToModSets.clear()
        modIdToModSets.putAll(
            modSets.entries.fold(mutableMapOf()) { map, curr ->
                for (mod in curr.value.mods) {
                    if (mod == curr.key) continue
                    val set = map.getOrPut(mod, ::mutableSetOf) as MutableSet
                    set += curr.key
                }
                map
            })

        runCatching {
            rulesDir.createDirectories()
        }

        runCatching {
            rules.clear()
            rulesDir.listDirectoryEntries("*.json").forEach {
                try {
                    rules[it.nameWithoutExtension] =
                        RuleSet.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(it.reader())).unwrap()
                } catch (e: Exception) {
                    ModSets.LOGGER.error("Failed to load rule ${it.name}", e)
                }
            }
        }
    }

    private fun save() {
        ModSetsConfig.save()
    }

    fun generateConfigScreen(lastScreen: Screen?): Screen =
        YetAnotherConfigLib(ModSets.ID) {
            ModSets.reload()
            runBlocking { reload() }

            title { Component.translatable("modsets.name") }

            save {
                ModSets.save()
                save()
            }

            if (rules.isNotEmpty()) {
                val options = mutableSetOf<Option<Any>>()

                for (ruleSetEntry in rules) {
                    categories.register(ruleSetEntry.key) {
                        val ruleSet = ruleSetEntry.value
                        name(ruleSet.text)
                        ruleSet.description?.let { tooltip(it) }
                        for (rule in ruleSet.rules) {
                            with(rule) { rule.controller.registerCategory() }
                        }

                        thisCategory.onReady { category ->
                            // Since the options are instant and may be affected by the others.
                            // Update the changed options to correct value
                            val optionsInCategory = category.groups().flatMap { it.options() }
                            options.addAll(optionsInCategory as MutableSet<Option<Any>>)
                        }
                    }
                }

                for (option in options) {
                    option.addListener { _, _ ->
                        var changed = false
                        for (anotherOption in options.filter { it != option && it.changed() }) {
                            anotherOption.requestSet(anotherOption.binding().value)
                            if (!changed && option.changed()) {
                                ModSets.LOGGER.warn(
                                    "Option ${option.name()} is conflicting with ${anotherOption.name()}. Can't change"
                                )
                                changed = true
                            }
                        }
                        if (option.changed()) {
                            ModSets.LOGGER.warn(
                                "Option ${option.name()} is conflicting with unknown option. Can't change"
                            )
                            option.requestSet(option.binding().value)
                        }
                        save() // The save won't be called with the instant
                    }
                }
            } else {
                categories.registering { name(Component.translatable("modsets.no_rules")) }
            }
        }
            .generateScreen(lastScreen)
}

@Suppress("DEPRECATION", "UnusedReceiverParameter")
val ModSetsIngameConfig.rules: ModSetsIngameConfig
    get() = ModSetsIngameConfig
