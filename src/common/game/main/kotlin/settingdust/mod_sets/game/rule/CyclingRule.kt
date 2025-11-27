package settingdust.mod_sets.game.rule

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.cyclingList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.HoverEvent
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.game.ModSetsIngameConfig
import settingdust.mod_sets.game.ModSetsIngameConfig.getOrThrow

/**
 * A cycling rule that allows selecting one option from a list of mod sets
 * This rule creates a cycling list controller in the UI
 *
 * @param ids The list of mod set IDs to cycle through
 */
@Serializable
@SerialName("cycling")
data class CyclingRule(val ids: List<String>) : RuleRegistrar {
    private val firstMod = ids.first()

    init {
        require(ids.isNotEmpty()) { "mod sets of cycling can't be empty" }
    }

    /**
     * Register this cycling rule as an option in the configuration UI
     * Creates a cycling list controller that allows selecting one mod set from the list
     *
     * @param rule The rule containing display information
     */
    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<String> {
            name(rule.text)
            (rule.description ?: ModSetsIngameConfig.modSets[firstMod]?.description)?.let {
                description(OptionDescription.of(it))
            }
            controller(cyclingList(ids) { mod ->
                val modSetDisplay = ModSetsIngameConfig.modSets.getOrThrow(mod)
                modSetDisplay.text.copy().withStyle { style ->
                    (modSetDisplay.description ?: ModSetsIngameConfig.modSets[mod]?.description)?.let {
                        style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, it))
                    }
                }
            })

            stateManager(
                StateManager.createInstant(
                    Binding.generic(
                        firstMod,
                        { getCurrentSelectedModSet() },
                        { id -> setSelectedModSet(id) }
                    )
                )
            )
        }
    }

    /**
     * Determine the currently selected mod set based on enabled mods
     *
     * @return The ID of the currently selected mod set
     */
    private fun getCurrentSelectedModSet(): String {
        val allModSets = ModSetsIngameConfig.modSets
        val enabledModSets = ids.filter { id ->
            if (id !in allModSets) return@filter false
            val mods = allModSets.getOrThrow(id).mods
            mods.none { it in ModSetsDisabledMods.disabledMods }
        }

        handleMultipleEnabledModSets(enabledModSets)

        val firstNonEmptyModSet = findFirstNonEmptyModSet()
        val modSetAllEnabled = findSingleEnabledModSet(enabledModSets)
        val selected = modSetAllEnabled ?: firstNonEmptyModSet ?: firstMod

        ModSetsDisabledMods.disabledMods.removeAll(allModSets.getOrThrow(selected).mods)
        return selected
    }

    /**
     * Handle the case where multiple mod sets are enabled simultaneously
     *
     * @param enabledModSets The list of currently enabled mod sets
     */
    private fun handleMultipleEnabledModSets(enabledModSets: List<String>) {
        if (enabledModSets.size > 1) {
            ModSets.LOGGER.warn(
                "More than one mod is enabled in cycling list: ${enabledModSets.joinToString()}. Will take the first and disable the others"
            )

            ModSetsDisabledMods.disabledMods.addAll(
                enabledModSets.drop(1).flatMap { ModSetsIngameConfig.modSets.getOrThrow(it).mods },
            )
            ModSetsDisabledMods.disabledMods.removeAll(
                ModSetsIngameConfig.modSets.getOrThrow(enabledModSets[0]).mods
            )
        } else if (enabledModSets.isEmpty()) {
            ModSets.LOGGER.warn(
                "None mod is enabled in cycling list: ${ids.joinToString()}. Will take the first and disable the others"
            )
        }
    }

    /**
     * Find the first non-empty mod set in the list
     *
     * @return The ID of the first non-empty mod set, or null if none found
     */
    private fun findFirstNonEmptyModSet(): String? {
        return ids.firstOrNull {
            it in ModSetsIngameConfig.modSets && ModSetsIngameConfig.modSets.getOrThrow(it).mods.isNotEmpty()
        }
    }

    /**
     * Find a single enabled mod set from the list of enabled mod sets
     *
     * @param enabledModSets The list of enabled mod sets
     * @return The ID of the single enabled mod set, or null if none or multiple found
     */
    private fun findSingleEnabledModSet(enabledModSets: List<String>): String? {
        return enabledModSets.singleOrNull { id ->
            ModSetsIngameConfig.modSets.getOrThrow(id).mods.none {
                it in ModSetsDisabledMods.disabledMods
            }
        }
    }

    /**
     * Set the selected mod set by enabling it and disabling others
     *
     * @param id The ID of the mod set to select
     */
    private fun setSelectedModSet(id: String) {
        ModSetsDisabledMods.disabledMods.addAll(
            ids.filter { it in ModSetsIngameConfig.modSets }
                .flatMap { ModSetsIngameConfig.modSets.getOrThrow(it).mods }
        )
        ModSetsDisabledMods.disabledMods.removeAll(
            ModSetsIngameConfig.modSets.getOrThrow(id).mods.toSet()
        )
    }
}
