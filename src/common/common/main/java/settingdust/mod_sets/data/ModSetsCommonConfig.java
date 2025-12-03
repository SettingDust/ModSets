package settingdust.mod_sets.data;

import settingdust.mod_sets.util.LoaderAdapter;
import settingdust.mod_sets.util.SavingData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModSetsCommonConfig implements SavingData {
    private static final Path configPath = LoaderAdapter.get().getConfigDirectory().resolve("common.json");
    public static Data data = new Data(true);

    @Override
    public void load() {
        try {Files.createDirectories(configPath.getParent());} catch (IOException ignored) {}
        try {
            Files.createFile(configPath);
            Files.writeString(configPath, "{}");
        } catch (IOException ignored) {}
        try {
            data = GSON.fromJson(Files.newBufferedReader(configPath), Data.class);
        } catch (IOException e) {
            throw new RuntimeException("Fail to load common config", e);
        }
        save();
    }

    @Override
    public void save() {
        try {
            GSON.toJson(data, Files.newBufferedWriter(configPath));
        } catch (IOException e) {
            throw new RuntimeException("Fail to save common config", e);
        }
    }

    public record Data(
        boolean badgeInModMenu
    ) {}
}
