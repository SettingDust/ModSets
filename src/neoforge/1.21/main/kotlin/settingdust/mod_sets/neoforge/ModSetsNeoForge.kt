package settingdust.mod_sets.neoforge

import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.Entrypoint
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(ModSets.ID)
object ModSetsNeoForge {
    init {
        requireNotNull(ModSets)
        Entrypoint.construct()
        MOD_BUS.apply {
            addListener<FMLCommonSetupEvent> {
                Entrypoint.init()
            }
            addListener<FMLClientSetupEvent> { Entrypoint.clientInit() }
        }
    }
}