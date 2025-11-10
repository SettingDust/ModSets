package settingdust.mod_sets.ingame.rule

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.GroupRegistrar
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import settingdust.mod_sets.ingame.util.GroupDslImpl

private const val DUMMY_ID = "_DUMMY_"

/**
 * Internal helper function to register a group without specifying an ID
 *
 * @param block The configuration block for the group
 */
private fun GroupRegistrar.register(block: GroupDsl.() -> Unit) = register(DUMMY_ID, block)

/**
 * Internal helper function to register a group without specifying an ID
 *
 * @param registrant The group to register
 */
private fun GroupRegistrar.register(registrant: OptionGroup) = register(DUMMY_ID, registrant)

/**
 * A rules group rule that organizes multiple sub-rules into a collapsible group
 * This rule allows creating hierarchical rule structures
 *
 * @param rules The list of sub-rules to include in this group
 * @param collapsed Whether the group should be collapsed by default
 */
@Serializable
@SerialName("rules_group")
data class RulesGroupRule(val rules: List<Rule>, val collapsed: Boolean = true) : RuleRegistrar {

    init {
        require(rules.isNotEmpty()) { "rules of rules_group can't be empty" }
    }

    /**
     * Register this rules group rule as a category in the configuration UI
     * Creates a collapsible group containing all sub-rules
     *
     * @param rule The rule containing display information
     * @param category The category DSL to register the group in
     */
    override fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.groups.register(
            OptionGroup.createBuilder()
                .apply {
                    collapsed(collapsed)
                    val group = createGroup(rule, category, this)
                    registerSubRules(group)
                }
                .build())
    }

    /**
     * Create a group DSL for this rules group
     *
     * @param rule The rule containing display information
     * @param category The parent category
     * @param builder The option group builder
     * @return The created group DSL
     */
    private fun createGroup(
        rule: Rule,
        category: CategoryDsl,
        builder: OptionGroup.Builder
    ): GroupDsl {
        return GroupDslImpl(DUMMY_ID, category, builder).apply {
            name(rule.text)
            rule.description?.let { description(OptionDescription.of(it)) }
        }
    }

    /**
     * Register all sub-rules in this group
     *
     * @param group The group DSL to register rules in
     */
    private fun registerSubRules(group: GroupDsl) {
        for (subRule in rules) {
            subRule.controller.registerGroup(subRule, group)
        }
    }

    /**
     * Register this rules group rule within a group in the configuration UI
     * Creates a label for the group and registers all sub-rules
     *
     * @param rule The rule containing display information
     * @param group The group DSL to register in
     */
    override fun registerGroup(rule: Rule, group: GroupDsl) {
        registerGroupLabel(rule, group)
        registerSubRulesInGroup(group)
    }

    /**
     * Register the group label in the configuration UI
     *
     * @param rule The rule containing display information
     * @param group The group DSL to register in
     */
    private fun registerGroupLabel(rule: Rule, group: GroupDsl) {
        group.options.register<Component> {
            name(rule.text)
            rule.description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable(rule.text)))
        }
    }

    /**
     * Register all sub-rules in the parent group
     *
     * @param group The parent group DSL to register rules in
     */
    private fun registerSubRulesInGroup(group: GroupDsl) {
        for (subRule in rules) {
            subRule.controller.registerGroup(subRule, group)
        }
    }

    /**
     * Register this rules group rule as an option in the configuration UI
     * This implementation is empty as rules groups don't directly create options
     *
     * @param rule The rule containing display information
     * @param optionRegistrar The registrar used to create the UI elements
     */
    override fun OptionRegistrar.registerOption(rule: Rule) {
        // RulesGroupRule doesn't directly register options, only groups
    }
}
