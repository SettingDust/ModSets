package settingdust.modsets.forge.platform;

import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import settingdust.modsets.PlatformHelper;

import java.nio.file.Path;

public class PlatformHelperForge implements PlatformHelper {

    @NotNull
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("modsets");
    }
}
