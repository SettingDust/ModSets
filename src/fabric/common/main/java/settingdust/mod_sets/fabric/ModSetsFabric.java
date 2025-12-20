package settingdust.mod_sets.fabric;

import com.google.common.collect.Sets;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import settingdust.mod_sets.ModSets;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.fabric.util.LoaderAdapter;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ModSet;
import settingdust.mod_sets.game.util.ModSetLoadCallback;

public class ModSetsFabric {
    public static void init() {
        LoaderAdapter.inGame = true;
        ModSetLoadCallback.EVENT.register(() -> {
            var modSets = ModSetsIngameConfig.modSets();
            for (var mod : FabricLoader.getInstance().getAllMods()) {
                if (mod.getOrigin().getKind() == ModOrigin.Kind.NESTED) continue;
                var metadata = mod.getMetadata();
                if (metadata.getType().equals("builtin")) continue;
                var id = metadata.getId();
                if (modSets.containsKey(id)) {
                    ModSets.LOGGER.warn("Duplicated mod set with mod id: {}", id);
                }
                modSets.putIfAbsent(
                    id,
                    new ModSet(
                        Component.literal(id),
                        getModDescription(id, metadata.getName(), metadata.getVersion().getFriendlyString()),
                        Sets.newHashSet(id)
                    )
                );
            }

            for (final var id : ModSetsDisabledMods.disablingMods()) {
                if (modSets.containsKey(id)) continue;
                modSets.put(
                    id,
                    new ModSet(Component.literal(id), getModDescription(id, id, "disabled"), Sets.newHashSet(id))
                );
            }
        });
    }

    private static Component getModDescription(String id, String fallback, String version) {
        MutableComponent description;
        var nameKey = "modmenu.nameTranslation." + id;
        boolean nameTranslationExists;
        try {
            nameTranslationExists = I18n.exists(nameKey);
        } catch (Exception e) {
            nameTranslationExists = false;
        }
        if (nameTranslationExists) {
            description = Component.translatable(nameKey);
        } else {
            description = Component.literal(fallback);
        }
        description.append(" " + id + "@" + version);
        return description;
    }
}
