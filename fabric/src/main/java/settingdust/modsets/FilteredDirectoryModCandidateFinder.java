package settingdust.modsets;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.discovery.ModDiscoverer;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import settingdust.modsets.ConfigKt;
import settingdust.modsets.ModSets;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class FilteredDirectoryModCandidateFinder extends DirectoryModCandidateFinder {
    private static final Class<?> clazzModScanTask;
    private static final Constructor<?> modScanTaskConstructor;
    private static final Method modScanTaskCompute;

    static {
        try {
            clazzModScanTask = Class.forName("net.fabricmc.loader.impl.discovery.ModDiscoverer$ModScanTask");
            modScanTaskConstructor = clazzModScanTask.getDeclaredConstructors()[0];
            modScanTaskConstructor.setAccessible(true);
            modScanTaskCompute = clazzModScanTask.getDeclaredMethod("compute");
            modScanTaskCompute.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ModDiscoverer discoverer;

    public FilteredDirectoryModCandidateFinder(Path path, boolean requiresRemap) {
        super(path, requiresRemap);
        this.discoverer = new ModDiscoverer(new VersionOverrides(), new DependencyOverrides(FabricLoader.getInstance().getConfigDir()));
    }

    @Override
    public void findCandidates(ModCandidateConsumer out) {
        super.findCandidates((final var path, final var requiresRemap) -> {
            ModCandidate candidate;
            try {
                candidate = (ModCandidate) modScanTaskCompute.invoke(modScanTaskConstructor.newInstance(discoverer, path, requiresRemap));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (ConfigKt.getConfig(ModSets.INSTANCE).getDisabledMods().contains(candidate.getId())) return;
            out.accept(path, requiresRemap);
        });
    }
}
