package settingdust.mod_sets.fabric.util;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.ModDiscoverer;
import settingdust.mod_sets.data.ModSetsDisabledMods;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilteredDirectoryModCandidateFinder extends DirectoryModCandidateFinder {
    public static final Map<String, List<String>> directoryModSets = new HashMap<>();
    private static final Class<?> clazzModScanTask;
    private static final Constructor<?> modScanTaskConstructor;
    private static final Method modScanTaskCompute;

    static {
        try {
            clazzModScanTask = Class.forName("net.fabricmc.loader.impl.discovery.ModDiscoverer$ModScanTask");
            modScanTaskConstructor = clazzModScanTask.getDeclaredConstructor(
                ModDiscoverer.class,
                List.class,
                Boolean.TYPE
            );
            modScanTaskConstructor.setAccessible(true);
            modScanTaskCompute = clazzModScanTask.getDeclaredMethod("compute");
            modScanTaskCompute.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ModDiscoverer discoverer;
    private final String path;

    public FilteredDirectoryModCandidateFinder(Path path, ModDiscoverer discoverer, boolean requiresRemap) {
        super(path, requiresRemap);
        this.discoverer = discoverer;
        this.path = FabricLoaderImpl.INSTANCE.getModsDirectory().toPath().relativize(path).toString();
    }

    @Override
    public void findCandidates(ModCandidateConsumer out) {
        super.findCandidates((final var path, final var requiresRemap) -> {
            ModCandidateImpl candidate;
            try {
                candidate = (ModCandidateImpl) modScanTaskCompute.invoke(modScanTaskConstructor.newInstance(
                    discoverer,
                    path,
                    requiresRemap
                ));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            directoryModSets.putIfAbsent(this.path, new ArrayList<>());
            directoryModSets.get(this.path).add(candidate.getId());
            if (ModSetsDisabledMods.Companion.getDisabledMods().contains(candidate.getId())) return;
            out.accept(path, requiresRemap);
        });
    }
}
