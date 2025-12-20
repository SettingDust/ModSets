package settingdust.mod_sets.forge.game;

import com.google.common.collect.Sets;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModsFolderLocator;
import settingdust.mod_sets.ModSets;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.forge.util.LoaderAdapter;
import settingdust.mod_sets.game.ModSetsConfigScreenGenerator;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ModSet;
import settingdust.mod_sets.game.util.ModSetLoadCallback;
import settingdust.preloading_tricks.lexforge.mod_candidate.DefinedModLocator;

@Mod(ModSets.ID)
public class ModSetsForge {
    public ModSetsForge() {
        LoaderAdapter.inGame = true;
        @SuppressWarnings("removal")
        var loadingContext = FMLJavaModLoadingContext.get();
        var modBus = loadingContext.getModEventBus();
        modBus.addListener((FMLCommonSetupEvent event) -> {
            loadingContext.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (minecraft, parent) -> ModSetsConfigScreenGenerator.generateScreen(parent))
            );

            var modSets = ModSetsIngameConfig.modSets();

            ModSetLoadCallback.EVENT.register(() -> {
                for (final var mod : ModList.get().getMods()) {
                    var provider = mod.getOwningFile().getFile().getProvider();
                    if (!(provider instanceof ModsFolderLocator || provider instanceof DefinedModLocator)) continue;
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
