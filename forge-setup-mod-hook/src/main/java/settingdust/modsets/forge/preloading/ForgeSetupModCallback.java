package settingdust.modsets.forge.preloading;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import settingdust.modsets.ModSetsConfig;
import settingdust.preloadingtricks.SetupModCallback;
import settingdust.preloadingtricks.forge.ForgeLanguageProviderCallback;

import java.util.List;

public class ForgeSetupModCallback implements SetupModCallback {
    public ForgeSetupModCallback() throws NoSuchFieldException, IllegalAccessException {
        final var fieldMods = ModFileInfo.class.getDeclaredField("mods");
        fieldMods.setAccessible(true);

        final var disabledMods = ModSetsConfig.INSTANCE.getDisabledMods();
        final var service = ForgeLanguageProviderCallback.ForgeModSetupService.INSTANCE;
        final var toRemove = Lists.<ModFile>newArrayList();
        for (ModFile modFile : service.all()) {
            if (modFile.getModFileInfo() instanceof ModFileInfo info) {
                List<IModInfo> filtered = modFile.getModInfos().stream()
                        .filter(it -> !disabledMods.contains(it.getModId()))
                        .toList();
                if (filtered.isEmpty()) {
                    toRemove.add(modFile);
                } else fieldMods.set(info, filtered);
            }
        }
        service.removeAll(toRemove);
    }
}
