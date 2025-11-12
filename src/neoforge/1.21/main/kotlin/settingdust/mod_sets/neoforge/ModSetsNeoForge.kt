package settingdust.mod_sets.neoforge

import net.minecraft.network.chat.Component
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.ingame.ModSet
import settingdust.mod_sets.ingame.ModSetsIngameConfig
import settingdust.mod_sets.ingame.util.ModSetLoadCallback
import settingdust.mod_sets.util.Entrypoint
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(ModSets.ID)
object ModSetsNeoForge {
    init {
        requireNotNull(ModSets)
        Entrypoint.construct()
        MOD_BUS.apply {
            addListener<FMLCommonSetupEvent> {
                LOADING_CONTEXT.registerExtensionPoint(IConfigScreenFactory::class.java) {
                    IConfigScreenFactory { _, parent -> ModSetsIngameConfig.generateConfigScreen(parent) }
                }

                val modSets = ModSetsIngameConfig.modSets
                ModSetLoadCallback.CALLBACK.register {
                    for (mod in ModList.get().mods) {
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
