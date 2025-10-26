package settingdust.mod_sets.forge

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.util.Entrypoint
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(ModSets.ID)
object ModSetsForge {
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