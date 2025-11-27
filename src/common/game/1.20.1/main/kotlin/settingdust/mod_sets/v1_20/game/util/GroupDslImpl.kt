package settingdust.mod_sets.v1_20.game.util

import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.dsl.CategoryDsl
import settingdust.mod_sets.game.util.yacl.GroupDslImpl

class GroupDslImpl(groupId: String, parent: CategoryDsl, builder: OptionGroup.Builder = OptionGroup.createBuilder()) :
    GroupDslImpl(groupId, parent, builder) {
    var collapsed: Boolean = false
        set(value) {
            field = value
            builder.collapsed(value)
        }

    override fun `mod_sets$collapsed`(collapsed: Boolean) {
        this.collapsed = collapsed
    }

    class Factory : GroupDslImpl.Factory {
        override fun create(
            groupId: String,
            parent: CategoryDsl,
            builder: OptionGroup.Builder
        ): GroupDslImpl = settingdust.mod_sets.v1_20.game.util.GroupDslImpl(groupId, parent, builder)
    }
}
