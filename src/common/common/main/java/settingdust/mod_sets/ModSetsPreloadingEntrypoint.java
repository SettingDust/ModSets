package settingdust.mod_sets;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.util.LoaderAdapter;
import settingdust.preloading_tricks.api.PreloadingEntrypoint;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;

public class ModSetsPreloadingEntrypoint implements PreloadingEntrypoint {

    public static final Logger LOGGER = LogManager.getLogger();

    public ModSetsPreloadingEntrypoint() {
        var oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ModSets.class.getClassLoader());
        ModSets.load();
        Thread.currentThread().setContextClassLoader(oldClassLoader);

        PreloadingTricksCallbacks.COLLECT_MOD_CANDIDATES.register(manager -> {
            try {
                var subDirectories = Files.find(
                    LoaderAdapter.get().getModsDirectory(),
                    1,
                    (path, basicFileAttributes) -> basicFileAttributes.isDirectory(),
                    FileVisitOption.FOLLOW_LINKS
                ).toList();

                LOGGER.info("Loading mods from {} sub-folders in 'mods' folder", subDirectories.size());
                LOGGER.debug(String.join(", ", Lists.transform(subDirectories, it -> it.getFileName().toString())));
                manager.addAll(
                    subDirectories.stream().flatMap(it -> {
                        try {
                            return Streams.stream(Files.newDirectoryStream(it, "*.jar"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        PreloadingTricksCallbacks.SETUP_MODS.register(manager -> manager.removeByIds(ModSetsDisabledMods.disabledMods()));
    }
}
