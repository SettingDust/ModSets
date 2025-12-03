package settingdust.mod_sets.neoforge.game;

import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import settingdust.mod_sets.ModSets;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.game.ModSetsConfigScreenGenerator;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ModSet;
import settingdust.mod_sets.game.util.ModSetLoadCallback;

@Mod(ModSets.ID)
public class ModSetsNeoForge {
    public ModSetsNeoForge(IEventBus modBus) {
        modBus.addListener((FMLCommonSetupEvent event) -> {
            ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (modContainer, parent) -> ModSetsConfigScreenGenerator.generateScreen(parent)
            );

            var modSets = ModSetsIngameConfig.modSets();

            ModSetLoadCallback.EVENT.register(() -> {
                for (final var mod : ModList.get().getMods()) {
                    var modId = mod.getModId();
                    if (modSets.containsKey(modId)) {
                        ModSets.LOGGER.warn("Duplicated mod set with mod id: {}", modId);
                    } else {
                        modSets.put(
                            modId,
                            new ModSet(
                                Component.literal(modId),
                                Component.literal(mod.getDisplayName() + " " + modId + "@" + mod.getVersion()),
                                Sets.newHashSet(modId)
                            )
                        );
                    }
                }

                for (final var mod : ModSetsDisabledMods.disablingMods()) {
                    if (modSets.containsKey(mod)) continue;
                    modSets.put(
                        mod,
                        new ModSet(
                            Component.literal(mod),
                            Component.literal(mod + "@disabled"),
                            Sets.newHashSet(mod)
                        )
                    );
                }
            });
        });
    }
}
