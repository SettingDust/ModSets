package settingdust.mod_sets.util

import net.minecraft.resources.ResourceLocation

interface MinecraftAdapter {
    companion object : MinecraftAdapter by ServiceLoaderUtil.findService()

    fun id(namespace: String, path: String): ResourceLocation
}