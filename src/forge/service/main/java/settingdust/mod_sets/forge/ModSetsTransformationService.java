package settingdust.mod_sets.forge;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.niofs.union.UnionPath;
import settingdust.mod_sets.ModSets;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;
import settingdust.preloading_tricks.api.modlauncher.ModLauncherPreloadingCallbacks;
import settingdust.preloading_tricks.lexforge.LexForgeModManager;
import settingdust.preloading_tricks.util.LoaderPredicates;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ModSetsTransformationService implements ITransformationService {
    public ModSetsTransformationService() {
        if (!LoaderPredicates.Forge.test()) return;
        ModLauncherPreloadingCallbacks.COLLECT_ADDITIONAL_DEPENDENCY_SOURCES.register(manager -> {
            try {
                var selfPath =
                    ((UnionPath) Path.of(
                        ModSetsTransformationService.class
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

        PreloadingTricksCallbacks.SETUP_MODS.register(_manager -> {
            if (!(_manager instanceof LexForgeModManager manager)) return;
            manager.removeIf(it -> it.getModFileInfo() != null &&
                                   it.getModFileInfo().getFileProperties().containsKey("connector:placeholder"));
        });
    }

    @Override
    public String name() {
        return "Mod Sets dummy";
    }

    @Override
    public void initialize(final IEnvironment environment) {

    }

    @Override
    public void onLoad(final IEnvironment env, final Set<String> otherServices) {

    }

    @Override
    public List<ITransformer> transformers() {
        return List.of();
    }
}
