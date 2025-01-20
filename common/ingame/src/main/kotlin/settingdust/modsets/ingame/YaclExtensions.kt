package settingdust.modsets.ingame

import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.dsl.CategoryDsl
import dev.isxander.yacl3.dsl.GroupDsl
import dev.isxander.yacl3.dsl.OptionRegistrar
import dev.isxander.yacl3.dsl.OptionRegistrarImpl
import dev.isxander.yacl3.dsl.addDefaultText
import net.minecraft.network.chat.Component
import settingdust.modsets.ModSets
import java.util.concurrent.CompletableFuture

class GroupDslImpl(
    override val groupId: String,
    private val parent: CategoryDsl,
    private val builder: OptionGroup.Builder = OptionGroup.createBuilder()
) : GroupDsl {
    override val groupKey = "${parent.categoryKey}.group.$groupId"

    override val thisGroup = CompletableFuture<OptionGroup>()

    override val built = thisGroup

    private val optionFutures = mutableMapOf<String, CompletableFuture<Option<*>>>()
    private fun createOptionFuture(id: String) = optionFutures.computeIfAbsent(id) { CompletableFuture() }

    init {
        builder.name(Component.translatable(groupKey))
    }

    override val options: OptionRegistrar = OptionRegistrarImpl(
        { option, id -> builder.option(option).also { createOptionFuture(id).complete(option) } },
        { id -> createOptionFuture(id) },
        groupKey,
    )

    override fun name(component: Component) {
        builder.name(component)
    }

    override fun name(block: () -> Component) = name(block())

    override fun description(description: OptionDescription) {
        builder.description(description)
    }

    override fun descriptionBuilder(block: OptionDescription.Builder.() -> Unit) {
        builder.description(OptionDescription.createBuilder().apply(block).build())
    }

    override fun OptionDescription.Builder.addDefaultText(lines: Int?) {
        addDefaultText("$groupKey.description", lines)
    }

    override fun build(): OptionGroup =
        builder.build().also {
            thisGroup.complete(it)
            checkUnresolvedFutures()
        }

    private fun checkUnresolvedFutures() {
        optionFutures.filterValues { !it.isDone }
            .forEach { ModSets.LOGGER.error("Future option ${parent.categoryId}/$groupId/${it.key} was referenced but was never built.") }
    }
}
