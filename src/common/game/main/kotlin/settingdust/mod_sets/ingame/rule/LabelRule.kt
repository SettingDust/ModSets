package settingdust.mod_sets.ingame.rule

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.OptionDsl
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.TextLineBuilderDsl
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

private const val DUMMY_ID = "_DUMMY_"

/**
 * Internal helper function to register a label without specifying an ID
 *
 * @param builder The builder function to configure the label
 */
internal fun OptionRegistrar.registerLabel(builder: TextLineBuilderDsl.() -> Unit) =
    registerLabel(DUMMY_ID, builder)

/**
 * Internal helper function to register an option without specifying an ID
 *
 * @param block The configuration block for the option
 */
internal fun <T> OptionRegistrar.register(block: OptionDsl<T>.() -> Unit) =
    register(DUMMY_ID) {
        block(this)
    }

/**
 * A label rule that displays static text in the configuration UI
 * This rule creates a label controller with no interactive elements
 */
@Serializable
@SerialName("label")
data object LabelRule : RuleRegistrar {
    /**
     * Register this label rule as an option in the configuration UI
     * Creates a label controller that displays static text
     *
     * @param rule The rule containing the text and description to display
     * @param optionRegistrar The registrar used to create the UI element
     */
    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<Component> {
            name(rule.text)
            rule.description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable<Component>(rule.text)))
        }
    }
}
