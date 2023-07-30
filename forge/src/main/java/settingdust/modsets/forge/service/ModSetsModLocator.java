package settingdust.modsets.forge.service;

import com.google.common.collect.Streams;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Inspired by https://github.com/Chaos02/SubFolderLoader/blob/main/src/main/java/com/chaos02/structuredmodloader/StructuredModLoader.java
public class ModSetsModLocator extends AbstractJarFileModLocator {
    public static Map<String, List<String>> directoryModSet = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(ModSetsModLocator.class);
    private List<Path> mods = new ArrayList<>();

    @Override
    protected ModFileOrException createMod(Path... path) {
        final var result = super.createMod(path);
        if (result.file() == null) return result;
        final var dirName = result.file().getFilePath().getParent().getFileName().toString();
        directoryModSet.putIfAbsent(dirName, new ArrayList<>());
        directoryModSet.get(dirName).addAll(result.file().getModInfos().stream().map(IModInfo::getModId).toList());
        return super.createMod(path);
    }

    @Override
    public Stream<Path> scanCandidates() {
        var modsDir = FMLPaths.GAMEDIR.get().resolve(FMLPaths.MODSDIR.get());
        try (var dirs = Files.list(modsDir).filter(Files::isDirectory)) {
            var dirList = dirs.toList();
            logger.info("Loading mods from {} sub dir in mods", dirList.size());
            logger.debug(String.join(",", dirList.stream().map(it -> it.getFileName().toString()).toList()));

            return dirList.stream().flatMap(it -> {
                try {
                    return Streams.stream(Files.newDirectoryStream(it, "*.jar"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "mod sets";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }
}
