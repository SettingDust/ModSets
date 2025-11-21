package settingdust.mod_sets.ingame.rule

import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.tickBox
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import settingdust.mod_sets.ingame.ModSetsIngameConfig

/**
 * A boolean rule that provides a simple on/off toggle for a mod set
 * This rule creates a tick box controller in the UI
 *
 * @param id The mod set ID that this rule controls
 */
@Serializable
@SerialName("boolean")
data class BooleanRule(val id: String) : RuleRegistrar {
    /**
     * Register this boolean rule as an option in the configuration UI
     * Creates a tick box controller that toggles the enabled state of the mod set
     *
     * @param rule The rule containing display information
     */
    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<Boolean> {
            name(rule.text)
            (rule.description ?: ModSetsIngameConfig.modSets[id]?.description)?.let {
                description(OptionDescription.of(it))
            }
            controller(tickBox())
            stateManager(StateManager.createInstant(id.booleanBinding))
        }
    }
}
