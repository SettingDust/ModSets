package settingdust.mod_sets.neoforge;

import cpw.mods.niofs.union.UnionPath;
import settingdust.mod_sets.ModSets;
import settingdust.preloading_tricks.api.PreloadingEntrypoint;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
import settingdust.preloading_tricks.util.LoaderPredicates;

import java.net.URISyntaxException;
import java.nio.file.Path;

public class ModSetsNeoForgePreloadingEntrypoint implements PreloadingEntrypoint {
    public ModSetsNeoForgePreloadingEntrypoint() {
        ModLauncherPreloadingCallbacks.COLLECT_ADDITIONAL_DEPENDENCY_SOURCES.register(manager -> {
            if (!LoaderPredicates.NeoForgeModLauncher.test()) return;
            try {
                var selfPath =
                    ((UnionPath) Path.of(
                        ModSetsNeoForgePreloadingEntrypoint.class
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
