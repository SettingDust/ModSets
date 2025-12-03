package settingdust.mod_sets.data;

import com.google.gson.reflect.TypeToken;
import settingdust.mod_sets.util.LoaderAdapter;
import settingdust.mod_sets.util.SavingData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ModSetsDisabledMods implements SavingData {
    private static final Path configPath = LoaderAdapter.get().getConfigDirectory().resolve("disabled_mods.json");
    private static Set<String> disabledMods = null;
    private static Set<String> disablingMods = null;

    public static Set<String> disabledMods() {
        return disabledMods;
    }

    public static Set<String> disablingMods() {
        return disablingMods;
    }

    @Override
    public void load() {
        try {Files.createDirectories(configPath.getParent());} catch (IOException ignored) {}
        try {
            Files.createFile(configPath);
            Files.writeString(configPath, "[]");
        } catch (IOException ignored) {}

        try {
            disabledMods = GSON.fromJson(
                Files.readString(configPath),
                (TypeToken<Set<String>>) TypeToken.getParameterized(Set.class, String.class)
            );
            if (disabledMods == null) disabledMods = new HashSet<>();
            if (disablingMods == null) disablingMods = disabledMods;
        } catch (IOException e) {
            throw new RuntimeException("Fail to load disabled mods", e);
        }
        save();
    }

    @Override
    public void save() {
        try {
            GSON.toJson(disabledMods, Files.newBufferedWriter(configPath));
        } catch (IOException e) {
            throw new RuntimeException("Fail to save disabled mods", e);
        }
    }
}
