package settingdust.mod_sets.ingame.rule

import dev.isxander.yacl3.api.Binding
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.ingame.ModSetsIngameConfig
import settingdust.mod_sets.ingame.ModSetsIngameConfig.getOrThrow

/**
 * A set of rules that can be grouped together in the configuration UI
 *
 * @property text The display name of the rule set
 * @property description Optional description shown as tooltip
 * @property rules List of rules in this set
 */
@Serializable
data class RuleSet(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val rules: List<Rule>,
)

/**
 * A single rule that defines a configuration option in the UI
 *
 * @property text The display name of the rule
 * @property description Optional description shown as tooltip
 * @property controller The registrar that handles UI creation and logic
 */
@Serializable
data class Rule(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val controller: RuleRegistrar,
)

/**
 * Extension property to create a boolean binding for a mod set ID
 * This binding connects the UI state with the actual mod enable/disable status
 *
 * @receiver The mod set ID to bind to
 * @return A Binding that controls the enabled state of mods in the set
 */
val String.booleanBinding: Binding<Boolean>
    get() {
        val mods = ModSetsIngameConfig.modSets.getOrThrow(this).mods.toSet()
        return Binding.generic(
            true,
            { isAnyModEnabled(mods) },
            { enabled -> setModSetEnabled(mods, enabled) }
        )
    }

/**
 * Check if any mod in the set is currently enabled
 *
 * @param mods The set of mod IDs to check
 * @return true if any mod is enabled, false otherwise
 */
private fun isAnyModEnabled(mods: Set<String>): Boolean {
    return mods.any { it !in ModSetsDisabledMods.disabledMods }
}

/**
 * Enable or disable all mods in the set
 *
 * @param mods The set of mod IDs to update
 * @param enabled true to enable all mods, false to disable all mods
 */
private fun setModSetEnabled(mods: Set<String>, enabled: Boolean) {
    if (enabled) {
        ModSetsDisabledMods.disabledMods.removeAll(mods)
    } else {
        ModSetsDisabledMods.disabledMods.addAll(mods)
    }
}
