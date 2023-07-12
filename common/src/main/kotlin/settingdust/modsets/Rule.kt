package settingdust.modsets

import dev.isxander.yacl.api.Binding
import dev.isxander.yacl.api.Option
import dev.isxander.yacl.api.OptionGroup
import dev.isxander.yacl.gui.controllers.LabelController
import dev.isxander.yacl.gui.controllers.TickBoxController
import dev.isxander.yacl.gui.controllers.cycling.CyclingListController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

interface Described {
    val text: Component
    val tooltip: Component?
}

@Serializable
data class ModSet(override val text: Component, override val tooltip: Component?, val mods: List<String>) :
    Described

@Serializable
data class RuleSet(override val text: Component, override val tooltip: Component?, val rules: List<Rule>) : Described

@Serializable
data class Rule(
    override val text: Component,
    override val tooltip: Component?,
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
data object LabelRule : OptionRule<Component> {
    override fun get(rule: Described) =
        Option.createBuilder(Component::class.java)
            .name(rule.text)
            .apply { rule.tooltip?.let { tooltip(it) } }
            .controller(::LabelController)
            .binding(Binding.immutable(rule.text))
            .build()!!
}

private val String.booleanBinding: Binding<Boolean>
    get() = Binding.generic(
        true,
        { !ModSets.config.disabledMods.contains(this) },
        {
            if (it) {
                ModSets.config.disabledMods.remove(this)
            } else {
                ModSets.config.disabledMods.add(this)
            }
        },
    )

@Serializable
@SerialName("boolean")
data class BooleanRule(val mod: String) : OptionRule<Boolean> {

    override fun get(rule: Described) =
        Option.createBuilder(Boolean::class.java)
            .name(rule.text)
            .apply { rule.tooltip?.let { tooltip(it) } }
            .controller(::TickBoxController)
            .binding(mod.booleanBinding)
            .build()!!
}

@Serializable
@SerialName("cycling")
data class CyclingRule(val mods: List<String>) : OptionRule<String> {
    private val firstMod = mods.first()

    override fun get(rule: Described): Option<String> {
        val option = Option.createBuilder(String::class.java).name(rule.text)
        return option.controller { CyclingListController(it, mods) }
            .apply { rule.tooltip?.let { tooltip(it) } }
            .binding(
                Binding.generic(
                    firstMod,
                    {
                        val enabled = mods.asSequence().filter { !ModSets.config.disabledMods.contains(it) }.toList()
                        if (enabled.size > 1) {
                            ModSets.logger.warn("More than one mod is enabled in cycling list: " + enabled.joinToString() + ". Will take the first and disable the others")
                            for (i in 1..<enabled.size) ModSets.config.disabledMods.add(enabled[i])
                            return@generic enabled.first()
                        }
                        val currentSelected = enabled.singleOrNull() ?: firstMod
                        ModSets.config.disabledMods.remove(currentSelected)
                        return@generic currentSelected
                    },
                ) { value: String ->
                    ModSets.config.disabledMods.addAll(mods)
                    ModSets.config.disabledMods.remove(value)
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
            group.option(option.controller(::TickBoxController).binding(mod.booleanBinding).build())
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
