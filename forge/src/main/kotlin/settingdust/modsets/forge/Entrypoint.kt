package settingdust.modsets.forge

import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.common.Mod
import settingdust.modsets.ModSets
import settingdust.modsets.rules
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

@Mod("mod_sets")
class Entrypoint {
    init {
        // Take from https://github.com/isXander/YetAnotherConfigLib/blob/1.20.x/dev/test-forge/src/main/java/dev/isxander/yacl/test/forge/ForgeTest.java
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent ->
                ModSets.rules.createScreen(parent)
            }
        }
    }
}
