package settingdust.mod_sets.v1_21.util;

import net.minecraft.resources.ResourceLocation;

public class MinecraftAdapter implements settingdust.mod_sets.util.MinecraftAdapter {
    @Override
    public ResourceLocation id(final String namespace, final String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
