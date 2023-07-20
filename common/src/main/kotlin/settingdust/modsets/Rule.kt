package settingdust.modsets

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

interface Described {
    val text: Component
    val description: Component?
}

@Serializable
data class ModSet(
    override val text: @Contextual Component,
    override val description: @Contextual Component?,
    val mods: List<String>,
) :
    Described

@Serializable
data class RuleSet(
    override val text: @Contextual Component,
    override val description: @Contextual Component?,
    val rules: List<Rule>,
) : Described

@Serializable
data class Rule(
    override val text: @Contextual Component,
    override val description: @Contextual Component?,
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

@Serializable
@SerialName("label")
object LabelRule : OptionRule<Component> {
    override fun get(rule: Described) =
        Option.createBuilder<Component>()
            .name(rule.text)
            .apply { rule.description?.let { description(OptionDescription.of(it)) } }
            .flag(OptionFlag.GAME_RESTART)
            .customController(::LabelController)
            .binding(Binding.immutable(rule.text))
            .build()!!
}

private val String.booleanBinding: Binding<Boolean>
    get() {
        val mods = ModSets.rules.modSets[this]!!.mods.toSet()
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

@Serializable
@SerialName("boolean")
data class BooleanRule(val mod: String) : OptionRule<Boolean> {

    override fun get(rule: Described) =
        Option.createBuilder<Boolean>()
            .name(rule.text)
            .apply {
                (rule.description ?: ModSets.rules.modSets[mod]?.description)?.let { description(OptionDescription.of(it)) }
            }
            .flag(OptionFlag.GAME_RESTART)
            .controller(TickBoxControllerBuilder::create)
            .binding(mod.booleanBinding)
            .build()!!
}

@Serializable
@SerialName("cycling")
data class CyclingRule(val mods: List<String>) : OptionRule<String> {
    private val firstMod = mods.first()

    @OptIn(ExperimentalStdlibApi::class)
    override fun get(rule: Described): Option<String> {
        val option = Option.createBuilder<String>().name(rule.text)
        return option.controller {
            CyclingListControllerBuilder.create(it)
                .values(mods)
                .valueFormatter { mod ->
                    val modSet = ModSets.rules.modSets[mod]!!
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
            .flag(OptionFlag.GAME_RESTART)
            .binding(
                Binding.generic(
                    firstMod,
                    {
                        val enabledModSet = mods.asSequence()
                            .filter { modSet -> ModSets.rules.modSets[modSet]!!.mods.all { it !in ModSets.config.disabledMods } }
                            .toList()
                        if (enabledModSet.size > 1) {
                            ModSets.logger.warn("More than one mod is enabled in cycling list: " + enabledModSet.joinToString() + ". Will take the first and disable the others")
                            for (i in 1..<enabledModSet.size) ModSets.config.disabledMods.add(enabledModSet[i])
                            return@generic enabledModSet.first()
                        }
                        val currentSelected = enabledModSet.singleOrNull() ?: firstMod
                        ModSets.config.disabledMods.remove(currentSelected)
                        return@generic currentSelected
                    },
                ) { value: String ->
                    ModSets.config.disabledMods.addAll(mods.flatMap { ModSets.rules.modSets[it]!!.mods })
                    ModSets.config.disabledMods.removeAll(ModSets.rules.modSets[value]!!.mods.toSet())
                },
            ).build()
    }
}

@Serializable
@SerialName("mods_group")
data class ModsGroupRule(val mods: List<String>, val collapsed: Boolean = true) : GroupRule {
    override fun get(rule: Described): OptionGroup {
        val group = OptionGroup.createBuilder().name(rule.text)
        rule.description?.let { group.description(OptionDescription.of(it)) }
        for (mod in mods) {
            val modSet = ModSets.rules.modSets[mod]!!
            val option = Option.createBuilder<Boolean>().name(modSet.text)
            modSet.description?.let { option.description(OptionDescription.of(it)) }
            group.option(
                option.controller(TickBoxControllerBuilder::create).binding(mod.booleanBinding)
                    .flag(OptionFlag.GAME_RESTART)
                    .build(),
            )
        }
        return group.collapsed(collapsed).build()
    }
}

@Serializable
@SerialName("rules_group")
data class RulesGroupRule(val rules: List<Rule>, val collapsed: Boolean = true) : GroupRule {
    override fun get(rule: Described): OptionGroup {
        val group = OptionGroup.createBuilder().name(rule.text)
        rule.description?.let { group.description(OptionDescription.of(it)) }
        for (currentRule in rules) {
            group.option((currentRule.controller as OptionRule<*>).get(rule))
        }
        return group.collapsed(collapsed).build()
    }
}
