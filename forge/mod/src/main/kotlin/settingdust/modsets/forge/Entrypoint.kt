package settingdust.modsets.forge

import net.minecraft.network.chat.Component
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLPaths
import net.minecraftforge.fml.loading.moddiscovery.ModsFolderLocator
import settingdust.modsets.ModSets
import settingdust.modsets.config
import settingdust.modsets.forge.service.ModSetsModLocator
import settingdust.modsets.game.ModSet
import settingdust.modsets.game.rules
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import kotlin.io.path.div

@Mod("mod_sets")
class Entrypoint {
    init {
        // Take from
        // https://github.com/isXander/YetAnotherConfigLib/blob/1.20.x/dev/test-forge/src/main/java/dev/isxander/yacl/test/forge/ForgeTest.java
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> ModSets.rules.createScreen(parent) }
        }

        val gameDir = FMLPaths.GAMEDIR.get()
        val modsPath = FMLPaths.MODSDIR.get()
        val modSets = ModSets.rules.modSets

        ModSets.rules.ModSetsRegisterCallbacks += {
            for ((key, value) in
                ModSetsModLocator.directoryModSet.mapValues {
                    ModSet(
                        Component.literal(it.key),
                        Component.literal(gameDir.relativize(modsPath / it.key).toString()),
                        it.value.toMutableSet(),
                    )
                }) {
                if (key in modSets)
                    ModSets.LOGGER.warn("Duplicate mod set with directory name: $key")
                modSets.putIfAbsent(key, value)
            }

            for (mod in ModList.get().mods) {
                val provider = mod.owningFile.file.provider
                if (provider !is ModsFolderLocator && provider !is ModSetsModLocator) continue
                if (mod.modId in modSets)
                    ModSets.LOGGER.warn("Duplicate mod set with directory name: ${mod.modId}")
                modSets.putIfAbsent(
                    mod.modId,
                    ModSet(
                        Component.literal(mod.modId),
                        Component.literal("${mod.displayName} ${mod.modId}@${mod.version}"),
                        mutableSetOf(mod.modId),
                    )
                )
            }

            ModSetsConfig.disabledMods.forEach {
                modSets.putIfAbsent(
                    it,
                    ModSet(
                        Component.literal(it),
                        Component.literal("$it@disabled"),
                        mutableSetOf(it),
                    )
                )
            }
        }
    }
}
