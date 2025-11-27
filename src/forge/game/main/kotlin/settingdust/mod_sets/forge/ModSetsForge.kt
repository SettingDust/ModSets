package settingdust.mod_sets.forge

import net.minecraft.network.chat.Component
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.loading.moddiscovery.ModsFolderLocator
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.game.ModSet
import settingdust.mod_sets.game.ModSetsIngameConfig
import settingdust.mod_sets.game.util.ModSetLoadCallback
import settingdust.mod_sets.util.Entrypoint
import settingdust.preloading_tricks.lexforge.mod_candidate.DefinedModLocator
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(ModSets.ID)
object ModSetsForge {
    init {
        requireNotNull(ModSets)
        Entrypoint.construct()
        MOD_BUS.apply {
            addListener<FMLCommonSetupEvent> {
                LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
                    ConfigScreenFactory { _, parent -> ModSetsIngameConfig.generateConfigScreen(parent) }
                }

                val modSets = ModSetsIngameConfig.modSets
                ModSetLoadCallback.CALLBACK.register {
                    for (mod in ModList.get().mods) {
                        val provider = mod.owningFile.file.provider
                        if (provider !is ModsFolderLocator && provider !is DefinedModLocator) continue
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

                    ModSetsDisabledMods.definedModSets.forEach {
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

                Entrypoint.init()
            }
            addListener<FMLClientSetupEvent> { Entrypoint.clientInit() }
        }
    }
}
