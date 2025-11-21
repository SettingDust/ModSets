package settingdust.mod_sets.ingame.rule

import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.OptionRegistrar
import kotlinx.serialization.Serializable

/**
 * Base interface for all rule registrars
 * A rule registrar is responsible for creating UI elements and handling logic for a specific rule type
 */
@Serializable
sealed interface RuleRegistrar {
    /**
     * Register this rule as a top-level category in the configuration UI
     *
     * @param rule The rule to register
     * @param category The category DSL to register the rule in
     */
    fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.rootOptions.registerOption(rule)
    }

    /**
     * Register this rule within a group in the configuration UI
     *
     * @param rule The rule to register
     * @param group The group DSL to register the rule in
     */
    fun registerGroup(rule: Rule, group: GroupDsl) {
        group.options.registerOption(rule)
    }

    /**
     * Register this rule as an option in the configuration UI
     * This method must be implemented by all rule registrar implementations
     *
     * @param rule The rule to register
     */
    fun OptionRegistrar.registerOption(rule: Rule)
}
