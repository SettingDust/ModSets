package settingdust.mod_sets.neoforge.util;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import settingdust.mod_sets.ModSets;

import java.nio.file.Path;

public class LoaderAdapter implements settingdust.mod_sets.util.LoaderAdapter {

    public static final Path configDirectory = FMLPaths.CONFIGDIR.get().resolve(ModSets.ID);

    public static boolean inGame = false;

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public boolean isModLoaded(final String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override
    public Path getConfigDirectory() {
        return configDirectory;
    }

    @Override
    public Path getModsDirectory() {
        return FMLPaths.MODSDIR.get();
    }

    @Override
    public boolean isInGame() {
        return inGame;
    }
}
