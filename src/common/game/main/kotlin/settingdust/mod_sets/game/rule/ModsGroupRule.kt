package settingdust.mod_sets.game.rule

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.GroupRegistrar
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.tickBox
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import settingdust.mod_sets.game.ModSetsIngameConfig
import settingdust.mod_sets.game.ModSetsIngameConfig.getOrThrow

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
 * A mods group rule that organizes multiple mod sets into a collapsible group
 * This rule can display mod sets as a group with optional nested mod details
 *
 * @param ids The list of mod set IDs to include in this group
 * @param collapsed Whether the group should be collapsed by default
 * @param showMods Whether to show individual mods within each mod set
 */
@Serializable
@SerialName("mods_group")
data class ModsGroupRule(
    val ids: List<String>,
    val collapsed: Boolean = true,
    val showMods: Boolean = true
) : RuleRegistrar {

    init {
        require(ids.isNotEmpty()) { "mods of mods_group can't be empty" }
    }

    /**
     * Register this mods group rule as a category in the configuration UI
     * Creates a collapsible group containing options for each mod set
     *
     * @param rule The rule containing display information
     * @param category The category DSL to register the group in
     */
    override fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.groups.register(
            OptionGroup.createBuilder()
                .apply {
                    name(rule.text)
                    rule.description?.let { description(OptionDescription.of(it)) }
                    collapsed(collapsed)

                    val optionRegistrar =
                        dev.isxander.yacl3.dsl.OptionRegistrarImpl(
                            { option, _ -> option(option) },
                            { _ -> error("Unsupported") },
                            DUMMY_ID,
                        )

                    optionRegistrar.registerOption(rule)
                }
                .build())
    }

    /**
     * Register this mods group rule within a group in the configuration UI
     * Creates a label for the group and registers individual mod sets
     *
     * @param rule The rule containing display information
     * @param group The group DSL to register in
     */
    override fun registerGroup(rule: Rule, group: GroupDsl) {
        registerGroupLabel(rule, group)
        super.registerGroup(rule, group)
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
            (rule.description ?: ModSetsIngameConfig.modSets[ids.first()]?.description)?.let {
                description(OptionDescription.of(it))
            }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable(rule.text)))
        }
    }

    /**
     * Register this mods group rule as options in the configuration UI
     * Creates individual toggle options for each mod set in the group
     *
     * @param rule The rule containing display information
     * @param optionRegistrar The registrar used to create the UI elements
     */
    override fun OptionRegistrar.registerOption(rule: Rule) {
        registerModSets()
    }

    /**
     * Register all mod sets in this group
     */
    private fun OptionRegistrar.registerModSets() {
        for (id in ids.filter { it in ModSetsIngameConfig.modSets }) {
            registerModSet(id)
            if (showMods) {
                registerNestedMods(id)
            }
        }
    }

    /**
     * Register nested mods within a mod set
     *
     * @param id The mod set ID to register nested mods for
     */
    private fun OptionRegistrar.registerNestedMods(id: String) {
        val nestedIds = ModSetsIngameConfig.modSets.getOrThrow(id).mods.filter {
            it != id && it in ModSetsIngameConfig.modSets
        }

        for (nestedId in nestedIds) {
            registerModSet(nestedId)
        }
    }

    /**
     * Register a single mod set as a toggle option
     *
     * @param id The mod set ID to register
     */
    private fun OptionRegistrar.registerModSet(id: String) {
        val modSetDisplay = ModSetsIngameConfig.modSets.getOrThrow(id)
        register<Boolean> {
            name(modSetDisplay.text)
            modSetDisplay.description?.let { description(OptionDescription.of(it)) }
            controller(tickBox())
            stateManager(StateManager.createInstant(id.booleanBinding))
        }
    }
}
