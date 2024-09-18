package settingdust.modsets.ingame

import dev.isxander.yacl3.api.Binding
import dev.isxander.yacl3.api.OptionAddable
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.GroupDslImpl
import dev.isxander.yacl3.dsl.GroupRegistrar
import dev.isxander.yacl3.dsl.OptionDsl
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.OptionRegistrarImpl
import dev.isxander.yacl3.dsl.TextLineBuilderDsl
import dev.isxander.yacl3.dsl.binding
import dev.isxander.yacl3.dsl.controller
import dev.isxander.yacl3.dsl.cyclingList
import dev.isxander.yacl3.dsl.tickBox
import dev.isxander.yacl3.gui.controllers.LabelController
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.quiltmc.qkl.library.serialization.annotation.CodecSerializable
import settingdust.modsets.ModSets
import settingdust.modsets.ModSetsConfig
import settingdust.modsets.ingame.ModSetsIngameConfig.getOrThrow

@CodecSerializable
data class RuleSet(
    val text: @Contextual Component,
    val description: @Contextual Component? = null,
    val rules: List<Rule>,
) {
    companion object {
        val CODEC = ModSets.CODEC_FACTORY.create<RuleSet>()
    }
}

@CodecSerializable
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
        instant(true)
        block(this)
    }

private fun GroupRegistrar.register(block: GroupDsl.() -> Unit) = register(DUMMY_ID, block)

private fun GroupRegistrar.register(registrant: OptionGroup) = register(DUMMY_ID, registrant)

@CodecSerializable
sealed interface RuleController {
    fun <T : OptionAddable> build(builder: T, rule: Rule): T
}

@CodecSerializable
sealed interface OptionRule<T> : RuleController {
    override fun <T : OptionAddable> build(builder: T, rule: Rule): T
}

@CodecSerializable
sealed interface GroupRule : RuleController {
    override fun <T : OptionAddable> build(builder: T, rule: Rule): T
}

@CodecSerializable
sealed interface RuleRegistrar {
    context(Rule, CategoryDsl)
    fun registerCategory() {
        with(rootOptions) { registerOption() }
    }

    context(Rule, GroupDsl)
    fun registerGroup() {
        with(options) { registerOption() }
    }

    context(Rule, OptionRegistrar)
    fun registerOption()
}

@CodecSerializable
@SerialName("label")
data object LabelRule : RuleRegistrar {
    context(Rule, OptionRegistrar)
    override fun registerOption() {
        register<Component> {
            name(text)
            description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            binding = Binding.immutable(text)
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

@CodecSerializable
@SerialName("label")
data class BooleanRule(val id: String) : RuleRegistrar {
    context(Rule, OptionRegistrar)
    override fun registerOption() {
        register<Boolean> {
            name(text)
            (description ?: ModSetsIngameConfig.modSets[id]?.description)?.let {
                description(OptionDescription.of(it))
            }
            controller = tickBox()
            binding = id.booleanBinding
        }
    }
}

@CodecSerializable
@SerialName("cycling")
data class CyclingRule(val ids: List<String>) : RuleRegistrar {
    private val firstMod = ids.first()

    init {
        require(ids.isNotEmpty()) { "mod sets of cycling can't be empty" }
    }

    context(Rule, OptionRegistrar)
    override fun registerOption() {
        register<String> {
            name(text)
            (description ?: ModSetsIngameConfig.modSets[firstMod]?.description)?.let {
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

            binding =
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
                                allModSets.getOrThrow(enabledModSets.first()).mods
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
        }
    }
}

@CodecSerializable
@SerialName("mods_group")
data class ModsGroupRule(
    val ids: List<String>,
    val collapsed: Boolean = true,
    val showMods: Boolean = true
) : RuleRegistrar {

    init {
        require(ids.isNotEmpty()) { "mods of mods_group can't be empty" }
    }

    context(Rule, CategoryDsl)
    override fun registerCategory() {
        groups.register(
            OptionGroup.createBuilder()
                .apply {
                    name(text)
                    description?.let { description(OptionDescription.of(it)) }
                    collapsed(collapsed)

                    val optionRegistrar =
                        OptionRegistrarImpl(
                            { option, _ -> option(option) },
                            { _ -> error("Unsupported") },
                            DUMMY_ID,
                        )

                    with(optionRegistrar) { registerOption() }
                }
                .build())
    }

    context(Rule, GroupDsl)
    override fun registerGroup() {
        options.register<Component> {
            name(text)
            (description ?: ModSetsIngameConfig.modSets[ids.first()]?.description)?.let {
                description(OptionDescription.of(it))
            }
            customController(::LabelController)
            binding = Binding.immutable(text)
        }
        super.registerGroup()
    }

    context(Rule, OptionRegistrar)
    override fun registerOption() {
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
            binding = id.booleanBinding
        }
    }
}

@CodecSerializable
@SerialName("rules_group")
data class RulesGroupRule(val rules: List<Rule>, val collapsed: Boolean = true) : RuleRegistrar {

    init {
        require(rules.isNotEmpty()) { "rules of rules_group can't be empty" }
    }

    context(Rule, CategoryDsl)
    override fun registerCategory() {
        groups.register(
            OptionGroup.createBuilder()
                .apply {
                    name(text)
                    description?.let { description(OptionDescription.of(it)) }
                    collapsed(collapsed)

                    with(GroupDslImpl(DUMMY_ID, this@CategoryDsl)) {
                        for (rule in rules) {
                            rule.controller.registerGroup()
                        }
                    }
                }
                .build())
    }

    context(Rule, GroupDsl)
    override fun registerGroup() {
        options.register<Component> {
            name(text)
            description?.let { description(OptionDescription.of(it)) }
            customController(::LabelController)
            binding = Binding.immutable(text)
        }
        for (rule in rules) {
            rule.controller.registerGroup()
        }
    }

    context(Rule, OptionRegistrar)
    override fun registerOption() {
    }
}
