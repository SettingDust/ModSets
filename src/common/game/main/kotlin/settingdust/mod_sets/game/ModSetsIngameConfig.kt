package settingdust.mod_sets.game

import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.dsl.RootDsl
import dev.isxander.yacl3.dsl.YetAnotherConfigLib
import dev.isxander.yacl3.dsl.onReady
import dev.isxander.yacl3.gui.YACLScreen
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import settingdust.kinecraft.serialization.GsonElementSerializer
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.game.rule.RuleSet
import settingdust.mod_sets.game.util.ModSetLoadCallback
import settingdust.mod_sets.util.LoaderAdapter
import settingdust.mod_sets.util.serialization.ComponentSerializer
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

/**
 * Represents a mod set that can be enabled or disabled as a group
 *
 * @property text The display name of the mod set
 * @property description Optional description shown as tooltip
 * @property mods The set of mod IDs that belong to this mod set
 */
@Serializable
data class ModSet(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val mods: MutableSet<String>,
)

/**
 * Main configuration object for the ModSets mod
 * Handles loading mod sets and rules, and generating the configuration UI
 */
object ModSetsIngameConfig {
    class SavingData : settingdust.mod_sets.data.SavingData {
        override fun reload() {
            ModSetsIngameConfig.reload()
        }

        override fun save() {}
    }

    // JSON serializer with custom serializers for components and Gson elements
    private val json = Json(ModSets.configJson) {
        serializersModule += SerializersModule {
            contextual(GsonElementSerializer)
            contextual(ComponentSerializer)
        }
    }

    // Path to the modsets.json configuration file
    private val modSetsPath = LoaderAdapter.configPath / "modsets.json"

    // Map of all loaded mod sets, keyed by their ID
    var modSets: MutableMap<String, ModSet> = mutableMapOf()
        private set

    // Map from mod ID to the mod sets that contain it
    val modIdToModSets = mutableMapOf<String, Set<ModSet>>()

    // Map of mod sets defined in the configuration file
    private var definedModSets = mutableMapOf<String, ModSet>()

    // Directory containing rule configuration files
    private val rulesDir = LoaderAdapter.configPath / "rules"

    // Map of all loaded rule sets, keyed by their filename (without extension)
    var rules: MutableMap<String, RuleSet> = mutableMapOf()
        private set

    // Helper function to get a mod set by name or throw an exception if it doesn't exist
    fun MutableMap<String, ModSet>.getOrThrow(name: String) =
        requireNotNull(get(name)) { "Mod sets $name not exist" }

    // Reload all configuration data from files
    fun reload() {
        ensureModSetsFileExists()
        loadModSetsFromFile()
        notifyModSetLoadListeners()
        buildModIdToModSetsMapping()
        ensureRulesDirectoryExists()
        loadRulesFromDirectory()
    }

    // Ensure modsets.json file exists
    private fun ensureModSetsFileExists() {
        runCatching {
            modSetsPath.createFile()
            modSetsPath.writeText("{}")
        }
    }

    // Load mod sets from configuration file
    private fun loadModSetsFromFile() {
        runCatching {
            definedModSets = json.decodeFromStream(modSetsPath.inputStream())
        }
        modSets.clear()
        modSets.putAll(definedModSets)
    }

    // Notify listeners that mod sets have been loaded
    private fun notifyModSetLoadListeners() {
        ModSetLoadCallback.CALLBACK.invoker.onModSetLoad()
    }

    // Build reverse mapping from mod ID to mod sets
    private fun buildModIdToModSetsMapping() {
        modIdToModSets.clear()
        modIdToModSets.putAll(
            modSets.entries.fold(mutableMapOf()) { map, curr ->
                for (mod in curr.value.mods) {
                    if (mod == curr.key) continue
                    val set = map.getOrPut(mod, ::mutableSetOf) as MutableSet
                    set += curr.value
                }
                map
            }
        )
    }

    // Ensure rules directory exists
    private fun ensureRulesDirectoryExists() {
        runCatching {
            rulesDir.createDirectories()
        }
    }

    // Load all rule files from the rules directory
    private fun loadRulesFromDirectory() {
        rules.clear()
        rulesDir.listDirectoryEntries("*.json").forEach { file ->
            try {
                rules[file.nameWithoutExtension] = json.decodeFromStream(file.inputStream())
            } catch (e: Exception) {
                ModSets.LOGGER.error("Failed to load rule ${file.name}", e)
            }
        }
    }

    // Save the current configuration
    private fun save() {
        ModSets.save()
    }

    // Generate the YACL configuration UI
    internal fun generateConfig() = YetAnotherConfigLib(ModSets.ID) {
        // Reload configuration data
        ModSets.reload()
        runBlocking { reload() }

        // Set the title of the configuration screen
        title { Component.translatable("modsets.name") }

        // Set the save callback
        save { save() }

        // Register rule sets if any exist, otherwise show a "no rules" message
        if (rules.isNotEmpty()) {
            registerRuleSets()
        } else {
            showNoRulesMessage()
        }
    }

    // Register all rule sets in the configuration UI
    private fun RootDsl.registerRuleSets() {
        val options = mutableSetOf<Option<Any>>()

        // Register each rule set as a category
        for (ruleSetEntry in rules) {
            categories.register(ruleSetEntry.key) {
                val ruleSet = ruleSetEntry.value
                name(ruleSet.text)
                ruleSet.description?.let { tooltip(it) }

                // Register each rule in the rule set
                for (rule in ruleSet.rules) {
                    rule.controller.registerCategory(rule, this@register)
                }

                // Collect all options in this category for conflict resolution
                thisCategory.onReady { category ->
                    val optionsInCategory = category.groups().flatMap { it.options() }
                    options.addAll(optionsInCategory as List<Option<Any>>)
                }
            }
        }

        // Add event listeners to all options for conflict resolution
        for (option in options) {
            option.addEventListener { _, _ ->
                handleOptionConflicts(option, options)
                if (option.changed()) {
                    option.requestSet(option.stateManager().get())
                }
                save() // The save won't be called with the instant
            }
        }
    }

    // Handle conflicts between options
    private fun handleOptionConflicts(currentOption: Option<Any>, allOptions: Set<Option<Any>>) {
        var changed = false
        for (anotherOption in allOptions.filter { it != currentOption && it.changed() }) {
            anotherOption.requestSet(anotherOption.stateManager().get())
            if (!changed && currentOption.changed()) {
                ModSets.LOGGER.warn(
                    "Option ${currentOption.name()} is conflicting with ${anotherOption.name()}. Can't change"
                )
                changed = true
            }
        }
        if (currentOption.changed() && !changed) {
            ModSets.LOGGER.warn(
                "Option ${currentOption.name()} is conflicting with unknown option. Can't change"
            )
        }
    }

    // Show a message when no rules are configured
    private fun RootDsl.showNoRulesMessage() {
        categories.register("no_rules") { name(Component.translatable("modsets.no_rules")) }
    }

    // Generate a configuration screen
    fun generateConfigScreen(lastScreen: Screen?): Screen =
        ModSetConfigScreen(lastScreen)

    // Custom YACLScreen implementation for mod sets configuration
    class ModSetConfigScreen(parent: Screen?) : YACLScreen(generateConfig(), parent)
}
