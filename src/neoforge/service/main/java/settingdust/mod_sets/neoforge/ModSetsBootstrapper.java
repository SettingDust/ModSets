package settingdust.mod_sets.neoforge;

import cpw.mods.niofs.union.UnionPath;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;
import settingdust.mod_sets.ModSets;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
import settingdust.preloading_tricks.util.LoaderPredicates;

import java.net.URISyntaxException;
import java.nio.file.Path;

public class ModSetsBootstrapper implements GraphicsBootstrapper {
    public ModSetsBootstrapper() {
        ModLauncherPreloadingCallbacks.COLLECT_ADDITIONAL_DEPENDENCY_SOURCES.register(manager -> {
            if (!LoaderPredicates.NeoForgeModLauncher.test()) return;
            try {
                var selfPath =
                    ((UnionPath) Path.of(
                        ModSetsBootstrapper.class
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

    @Override
    public String name() {
        return "Mod Sets dummy";
    }

    @Override
    public void bootstrap(final String[] arguments) {

    }
}
