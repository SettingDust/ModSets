package settingdust.mod_sets.fabric.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import settingdust.mod_sets.ModSets;

import java.nio.file.Path;

public class LoaderAdapter implements settingdust.mod_sets.util.LoaderAdapter {
    private final Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(ModSets.ID);
    private final Path modsDirectory = FabricLoaderImpl.INSTANCE.getModsDirectory().toPath();

    public static boolean inGame = false;

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isModLoaded(final String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDirectory() {
        return configDirectory;
    }

    @Override
    public Path getModsDirectory() {
        return modsDirectory;
    }

    @Override
    public boolean isInGame() {
        return inGame;
    }
}
