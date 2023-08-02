package settingdust.modsets.game

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import settingdust.modsets.ModSets
import settingdust.modsets.config
import settingdust.modsets.game.Rules.getOrThrow

interface Described {
    val text: Component
    val description: Component?
}

@Serializable
data class ModSet(
    override val text: @Contextual Component,
    override val description: @Contextual Component? = null,
    val mods: MutableSet<String>,
) :
    Described

@Serializable
data class RuleSet(
    override val text: @Contextual Component,
    override val description: @Contextual Component? = null,
    val rules: List<Rule>,
) : Described

@Serializable
data class Rule(
    override val text: @Contextual Component,
    override val description: @Contextual Component? = null,
    val controller: RuleController,
) : Described

@Serializable
sealed interface RuleController {
    fun get(rule: Described): Any
}

@Serializable
sealed interface OptionRule<T> : RuleController {
    override fun get(rule: Described): Option<T>
}

@Serializable
sealed interface GroupRule : RuleController {
    override fun get(rule: Described): OptionGroup
}

@Suppress("unused")
@Serializable
@SerialName("label")
object LabelRule : OptionRule<Component> {
    override fun get(rule: Described) =
        Option.createBuilder<Component>()
            .name(rule.text)
            .apply { rule.description?.let { description(OptionDescription.of(it)) } }
            .customController(::LabelController)
            .binding(Binding.immutable(rule.text))
            .build()!!
}

private val String.booleanBinding: Binding<Boolean>
    get() {
        val mods = ModSets.rules.modSets.getOrThrow(this).mods.toSet()
        return Binding.generic(
            true,
            { mods.any { it !in ModSets.config.disabledMods } },
            {
                if (it) {
                    ModSets.config.disabledMods.removeAll(mods)
                } else {
                    ModSets.config.disabledMods.addAll(mods)
                }
            },
        )
    }

@Suppress("unused")
@Serializable
@SerialName("boolean")
data class BooleanRule(val mod: String) : OptionRule<Boolean> {

    override fun get(rule: Described) =
        Option.createBuilder<Boolean>()
            .name(rule.text)
            .apply {
                (
                        rule.description
                            ?: ModSets.rules.modSets[mod]?.description
                        )?.let { description(OptionDescription.of(it)) }
            }
            .instant(true)
            .controller(TickBoxControllerBuilder::create)
            .binding(mod.booleanBinding)
            .build()!!
}

@Suppress("unused")
@Serializable
@SerialName("cycling")
data class CyclingRule(val mods: List<String>) : OptionRule<String> {
    private val firstMod = mods.first()

    init {
        require(mods.isNotEmpty()) { "mods of cycling can't be empty" }
    }

    override fun get(rule: Described): Option<String> {
        val option = Option.createBuilder<String>().name(rule.text)
        return option.controller {
            CyclingListControllerBuilder.create(it)
                .values(mods)
                .valueFormatter { mod ->
                    val modSet = ModSets.rules.modSets.getOrThrow(mod)
                    modSet.text.copy()
                        .withStyle(
                            Style.EMPTY.withHoverEvent(
                                modSet.description?.let { tooltip ->
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        tooltip,
                                    )
                                },
                            ),
                        )
                }
        }
            .apply { rule.description?.let { description(OptionDescription.of(it)) } }
            .instant(true)
            .binding(
                Binding.generic(
                    firstMod,
                    {
                        val modSets = ModSets.rules.modSets
                        val enabledModSet = mods.asSequence()
                            .filter { modSet ->
                                val mods = modSets.getOrThrow(modSet).mods
                                mods.isNotEmpty() && mods.none { it in ModSets.config.disabledMods }
                            }
                            .toList()
                        if (enabledModSet.size > 1) {
                            ModSets.logger.warn("More than one mod is enabled in cycling list: " + enabledModSet.joinToString() + ". Will take the first and disable the others")
                            ModSets.config.disabledMods.addAll(
                                enabledModSet.drop(1).flatMap { modSets.getOrThrow(it).mods },
                            )
                            ModSets.config.disabledMods.removeAll(modSets.getOrThrow(enabledModSet.first()).mods.toSet())
                            return@generic enabledModSet.first()
                        }
                        val currentSelected =
                            enabledModSet.singleOrNull { modSet ->
                                modSets.getOrThrow(modSet).mods.none { it in ModSets.config.disabledMods }
                            } ?: mods.firstOrNull { modSets.getOrThrow(it).mods.isEmpty() }
                            ?: firstMod

                        ModSets.config.disabledMods.removeAll(modSets.getOrThrow(currentSelected).mods.toSet())
                        return@generic currentSelected
                    },
                ) { value: String ->
                    ModSets.config.disabledMods.addAll(mods.flatMap { ModSets.rules.modSets.getOrThrow(it).mods })
                    ModSets.config.disabledMods.removeAll(ModSets.rules.modSets.getOrThrow(value).mods.toSet())
                },
            ).build()
    }
}

@Suppress("unused")
@Serializable
@SerialName("mods_group")
data class ModsGroupRule(val mods: List<String>, val collapsed: Boolean = true) : GroupRule {

    init {
        require(mods.isNotEmpty()) { "mods of mods_group can't be empty" }
    }

    override fun get(rule: Described): OptionGroup {
        val group = OptionGroup.createBuilder().name(rule.text)
        rule.description?.let { group.description(OptionDescription.of(it)) }
        for (mod in mods) {
            val modSet = ModSets.rules.modSets.getOrThrow(mod)
            val option = Option.createBuilder<Boolean>().name(modSet.text)
            modSet.description?.let { option.description(OptionDescription.of(it)) }
            group.option(
                option.controller(TickBoxControllerBuilder::create).binding(mod.booleanBinding)
                    .instant(true)
                    .build(),
            )
        }
        return group.collapsed(collapsed).build()
    }
}

@Serializable
@SerialName("rules_group")
data class RulesGroupRule(val rules: List<Rule>, val collapsed: Boolean = true) : GroupRule {

    init {
        require(rules.isNotEmpty()) { "rules of rules_group can't be empty" }
    }

    override fun get(rule: Described): OptionGroup {
        val group = OptionGroup.createBuilder().name(rule.text)
        rule.description?.let { group.description(OptionDescription.of(it)) }
        for (currentRule in rules) {
            group.option((currentRule.controller as OptionRule<*>).get(rule))
        }
        return group.collapsed(collapsed).build()
    }
}
