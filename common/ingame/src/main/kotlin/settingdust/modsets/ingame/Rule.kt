package settingdust.modsets.ingame

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionAddable
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.StateManager
import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.GroupRegistrar
import dev.isxander.yacl3.dsl.OptionDsl
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.OptionRegistrarImpl
import dev.isxander.yacl3.dsl.TextLineBuilderDsl
import dev.isxander.yacl3.dsl.controller
import dev.isxander.yacl3.dsl.cyclingList
import dev.isxander.yacl3.dsl.tickBox
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import settingdust.modsets.ModSets
import settingdust.modsets.ModSetsConfig
import settingdust.modsets.ingame.ModSetsIngameConfig.getOrThrow

@Serializable
data class RuleSet(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val rules: List<Rule>,
)

@Serializable
data class Rule(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val controller: RuleRegistrar,
)

private const val DUMMY_ID = "_DUMMY_"

private fun OptionRegistrar.registerLabel(builder: TextLineBuilderDsl.() -> Unit) =
    registerLabel(DUMMY_ID, builder)

private fun <T> OptionRegistrar.register(block: OptionDsl<T>.() -> Unit) =
    register<T>(DUMMY_ID) {
        block(this)
    }

private fun GroupRegistrar.register(block: GroupDsl.() -> Unit) = register(DUMMY_ID, block)

private fun GroupRegistrar.register(registrant: OptionGroup) = register(DUMMY_ID, registrant)

@Serializable
sealed interface RuleController {
    fun <T : OptionAddable> build(builder: T, rule: Rule): T
}

@Serializable
sealed interface RuleRegistrar {
    fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.rootOptions.registerOption(rule)
    }

    fun registerGroup(rule: Rule, group: GroupDsl) {
        group.options.registerOption(rule)
    }

    fun OptionRegistrar.registerOption(rule: Rule)
}

@Serializable
@SerialName("label")
data object LabelRule : RuleRegistrar {
    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<Component> {
            name(rule.text)
            rule.description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable<Component>(rule.text)))
        }
    }
}

private val String.booleanBinding: Binding<Boolean>
    get() {
        val mods = ModSetsIngameConfig.modSets.getOrThrow(this).mods.toSet()
        return Binding.generic(
            true,
            { mods.any { it !in ModSetsConfig.disabledMods } },
            {
                if (it) {
                    ModSetsConfig.disabledMods.removeAll(mods)
                } else {
                    ModSetsConfig.disabledMods.addAll(mods)
                }
            },
        )
    }

@Serializable
@SerialName("boolean")
data class BooleanRule(val id: String) : RuleRegistrar {
    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<Boolean> {
            name(rule.text)
            (rule.description ?: ModSetsIngameConfig.modSets[id]?.description)?.let {
                description(OptionDescription.of(it))
            }
            controller = tickBox()
            stateManager(StateManager.createInstant(id.booleanBinding))
        }
    }
}

@Serializable
@SerialName("cycling")
data class CyclingRule(val ids: List<String>) : RuleRegistrar {
    private val firstMod = ids.first()

    init {
        require(ids.isNotEmpty()) { "mod sets of cycling can't be empty" }
    }

    override fun OptionRegistrar.registerOption(rule: Rule) {
        register<String> {
            name(rule.text)
            (rule.description ?: ModSetsIngameConfig.modSets[firstMod]?.description)?.let {
                description(OptionDescription.of(it))
            }
            controller =
                cyclingList(ids) { mod ->
                    val modSetDisplay = ModSetsIngameConfig.modSets.getOrThrow(mod)
                    modSetDisplay.text.copy().withStyle { style ->
                        (modSetDisplay.description ?: ModSetsIngameConfig.modSets[mod]?.description)?.let {
                            style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, it))
                        }
                    }
                }


            stateManager(
                StateManager.createInstant(
                    Binding.generic(
                        firstMod,
                        {
                            val allModSets = ModSetsIngameConfig.modSets
                            val enabledModSets =
                                ids.filter { id ->
                                    if (id !in allModSets) return@filter false
                                    val mods = allModSets.getOrThrow(id).mods
                                    mods.none { it in ModSetsConfig.disabledMods }
                                }
                            if (enabledModSets.size > 1) {
                                ModSets.LOGGER.warn(
                                    "More than one mod is enabled in cycling list: ${enabledModSets.joinToString()}. Will take the first and disable the others"
                                )

                                ModSetsConfig.disabledMods.addAll(
                                    enabledModSets.drop(1).flatMap { allModSets.getOrThrow(it).mods },
                                )
                                ModSetsConfig.disabledMods.removeAll(
                                    allModSets.getOrThrow(enabledModSets[0]).mods
                                )
                                return@generic enabledModSets.first()
                            } else if (enabledModSets.isEmpty()) {
                                ModSets.LOGGER.warn(
                                    "None mod is enabled in cycling list: ${ids.joinToString()}. Will take the first and disable the others"
                                )
                            }
                            val firstNonEmptyModSet by lazy {
                                ids.firstOrNull {
                                    it in allModSets && allModSets.getOrThrow(it).mods.isNotEmpty()
                                }
                            }
                            val modSetAllEnabled =
                                enabledModSets.singleOrNull { id ->
                                    allModSets.getOrThrow(id).mods.none {
                                        it in ModSetsConfig.disabledMods
                                    }
                                }
                            val selected = modSetAllEnabled ?: firstNonEmptyModSet ?: firstMod

                            ModSetsConfig.disabledMods.removeAll(allModSets.getOrThrow(selected).mods)

                            return@generic selected
                        },
                        { id ->
                            ModSetsConfig.disabledMods.addAll(
                                ids.filter { it in ModSetsIngameConfig.modSets }
                                    .flatMap { ModSetsIngameConfig.modSets.getOrThrow(it).mods })
                            ModSetsConfig.disabledMods.removeAll(
                                ModSetsIngameConfig.modSets.getOrThrow(id).mods.toSet()
                            )
                        })
                )
            )
        }
    }
}

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

    override fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.groups.register(
            OptionGroup.createBuilder()
                .apply {
                    name(rule.text)
                    rule.description?.let { description(OptionDescription.of(it)) }
                    collapsed(collapsed)

                    val optionRegistrar =
                        OptionRegistrarImpl(
                            { option, _ -> option(option) },
                            { _ -> error("Unsupported") },
                            DUMMY_ID,
                        )

                    optionRegistrar.registerOption(rule)
                }
                .build())
    }

    override fun registerGroup(rule: Rule, group: GroupDsl) {
        group.options.register<Component> {
            name(rule.text)
            (rule.description ?: ModSetsIngameConfig.modSets[ids.first()]?.description)?.let {
                description(OptionDescription.of(it))
            }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable(rule.text)))
        }
        super.registerGroup(rule, group)
    }

    override fun OptionRegistrar.registerOption(rule: Rule) {
        for (id in ids.filter { it in ModSetsIngameConfig.modSets }) {
            registerModSet(id)
            if (showMods) {
                for (nestedId in
                ModSetsIngameConfig.modSets.getOrThrow(id).mods.filter {
                    it != id && it in ModSetsIngameConfig.modSets
                }) {
                    registerModSet(nestedId)
                }
            }
        }
    }

    private fun OptionRegistrar.registerModSet(id: String) {
        val modSetDisplay = ModSetsIngameConfig.modSets.getOrThrow(id)
        register<Boolean> {
            name(modSetDisplay.text)
            modSetDisplay.description?.let { description(OptionDescription.of(it)) }
            controller = tickBox()
            stateManager(StateManager.createInstant(id.booleanBinding))
        }
    }
}

@Serializable
@SerialName("rules_group")
data class RulesGroupRule(val rules: List<Rule>, val collapsed: Boolean = true) : RuleRegistrar {

    init {
        require(rules.isNotEmpty()) { "rules of rules_group can't be empty" }
    }

    override fun registerCategory(rule: Rule, category: CategoryDsl) {
        category.groups.register(
            OptionGroup.createBuilder()
                .apply {
                    collapsed(collapsed)

                    val group = GroupDslImpl(DUMMY_ID, category, this).apply {
                        name(rule.text)
                        rule.description?.let { description(OptionDescription.of(it)) }
                    }
                    for (rule in rules) {
                        rule.controller.registerGroup(rule, group)
                    }
                }
                .build())
    }

    override fun registerGroup(rule: Rule, group: GroupDsl) {
        group.options.register<Component> {
            name(rule.text)
            rule.description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            stateManager(StateManager.createInstant(Binding.immutable(rule.text)))
        }
        for (rule in rules) {
            rule.controller.registerGroup(rule, group)
        }
    }

    override fun OptionRegistrar.registerOption(rule: Rule) {
    }
}
