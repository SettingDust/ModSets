package settingdust.mod_sets.util;

import com.google.common.base.Suppliers;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public interface MinecraftAdapter {
    Supplier<MinecraftAdapter> supplier =
        Suppliers.memoize(() -> ServiceLoaderUtil.findService(MinecraftAdapter.class));

    static MinecraftAdapter get() {
        return supplier.get();
    }

    ResourceLocation id(String namespace, String path);
}
