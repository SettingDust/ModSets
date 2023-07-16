package settingdust.modsets

import dev.isxander.yacl.api.Binding
import dev.isxander.yacl.api.Option
import dev.isxander.yacl.api.OptionFlag
import dev.isxander.yacl.api.OptionGroup
import dev.isxander.yacl.gui.controllers.LabelController
import dev.isxander.yacl.gui.controllers.TickBoxController
import dev.isxander.yacl.gui.controllers.cycling.CyclingListController
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

interface Described {
    val text: Component
    val tooltip: Component?
}

@Serializable
data class ModSet(
    override val text: @Contextual Component,
    override val tooltip: @Contextual Component?,
    val mods: List<String>,
) :
    Described

@Serializable
data class RuleSet(
    override val text: @Contextual Component,
    override val tooltip: @Contextual Component?,
    val rules: List<Rule>,
) : Described

@Serializable
data class Rule(
    override val text: @Contextual Component,
    override val tooltip: @Contextual Component?,
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
        Option.createBuilder(Component::class.java)
            .name(rule.text)
            .apply { rule.tooltip?.let { tooltip(it) } }
            .flag(OptionFlag.GAME_RESTART)
            .controller(::LabelController)
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
        Option.createBuilder(Boolean::class.java)
            .name(rule.text)
            .apply { (rule.tooltip ?: ModSets.rules.modSets[mod]?.tooltip)?.let { tooltip(it) } }
            .flag(OptionFlag.GAME_RESTART)
            .controller(::TickBoxController)
            .binding(mod.booleanBinding)
            .build()!!
}

@Serializable
@SerialName("cycling")
data class CyclingRule(val mods: List<String>) : OptionRule<String> {
    private val firstMod = mods.first()

    @OptIn(ExperimentalStdlibApi::class)
    override fun get(rule: Described): Option<String> {
        val option = Option.createBuilder(String::class.java).name(rule.text)
        return option.controller {
            CyclingListController(it, mods) { mod ->
                val modSet = ModSets.rules.modSets[mod]!!
                modSet.text.copy()
                    .withStyle(
                        Style.EMPTY.withHoverEvent(
                            modSet.tooltip?.let { tooltip ->
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    tooltip,
                                )
                            },
                        ),
                    )
            }
        }
            .apply { rule.tooltip?.let { tooltip(it) } }
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
        rule.tooltip?.let { group.tooltip(it) }
        for (mod in mods) {
            val modSet = ModSets.rules.modSets[mod]!!
            val option = Option.createBuilder(Boolean::class.java).name(modSet.text)
            modSet.tooltip?.let { option.tooltip(it) }
            group.option(
                option.controller(::TickBoxController).binding(mod.booleanBinding).flag(OptionFlag.GAME_RESTART)
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
        rule.tooltip?.let { group.tooltip(it) }
        for (currentRule in rules) {
            group.option((currentRule.controller as OptionRule<*>).get(rule))
        }
        return group.collapsed(collapsed).build()
    }
}
