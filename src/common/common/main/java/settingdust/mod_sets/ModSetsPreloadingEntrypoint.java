package settingdust.mod_sets;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.util.LoaderAdapter;
import settingdust.preloading_tricks.api.PreloadingEntrypoint;
import settingdust.preloading_tricks.api.PreloadingTricksCallbacks;

import java.io.IOException;
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
                var subDirectories = Lists.newArrayList(Files.newDirectoryStream(
                    LoaderAdapter.get().getModsDirectory(),
                    it -> Files.isDirectory(it) && it.getFileName().toString().charAt(0) != '.'
                ));

                var subDirectoriesString = String.join(
                    ", ",
                    Lists.transform(subDirectories, it -> it.getFileName().toString())
                );
                System.setProperty(
                    "connector.additionalModLocations",
                    System.getProperty("connectoModuleClassLoaderr.additionalModLocations") + "," +
                    subDirectoriesString
                );

                LOGGER.info("Loading mods from {} sub-folders in 'mods' folder", subDirectories.size());
                LOGGER.debug(subDirectoriesString);
                for (final var directory : subDirectories) {
                    try (var files = Files.newDirectoryStream(directory, "*.jar")) {
                        files.forEach(manager::add);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        PreloadingTricksCallbacks.SETUP_MODS.register(manager -> manager.removeByIds(ModSetsDisabledMods.disabledMods()));
    }
}
