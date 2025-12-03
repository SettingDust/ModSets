package settingdust.mod_sets.forge;

import cpw.mods.niofs.union.UnionPath;
import settingdust.mod_sets.ModSets;
import settingdust.preloading_tricks.api.PreloadingEntrypoint;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;

import java.net.URISyntaxException;
import java.nio.file.Path;

public class ModSetsForgePreloadingEntrypoint implements PreloadingEntrypoint {
    public ModSetsForgePreloadingEntrypoint() {
        ModLauncherPreloadingCallbacks.COLLECT_ADDITIONAL_DEPENDENCY_SOURCES.register(manager -> {
            try {
                var selfPath =
                    ((UnionPath) Path.of(
                        ModSetsForgePreloadingEntrypoint.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()))
                        .getFileSystem().getPrimaryPath();
                manager.add(selfPath, ModSets.ID + "_service");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
