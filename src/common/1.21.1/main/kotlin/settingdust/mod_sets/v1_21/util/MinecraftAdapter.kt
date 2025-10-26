package settingdust.mod_sets.v1_21.util

import net.minecraft.resources.ResourceLocation
import settingdust.mod_sets.util.MinecraftAdapter

class MinecraftAdapter : MinecraftAdapter {
    override fun id(namespace: String, path: String) = ResourceLocation.fromNamespaceAndPath(namespace, path)
}