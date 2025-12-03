package settingdust.mod_sets;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settingdust.mod_sets.util.MinecraftAdapter;
import settingdust.mod_sets.util.SavingData;

public class ModSets {
    public static final String ID = "mod_sets";

    public static final Logger LOGGER = LoggerFactory.getLogger("ModSets");

    public static void load() {
        SavingData.invoker.load();
    }

    public static void save() {
        SavingData.invoker.save();
    }

    public static ResourceLocation id(String path) {
        return MinecraftAdapter.get().id(ID, path);
    }
}
