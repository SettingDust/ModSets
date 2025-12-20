package settingdust.mod_sets.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FilenameUtils;
import settingdust.mod_sets.ModSets;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.ModSet;
import settingdust.mod_sets.game.data.RuleSet;
import settingdust.mod_sets.game.util.ComponentAdapter;
import settingdust.mod_sets.game.util.ModSetLoadCallback;
import settingdust.mod_sets.util.LoaderAdapter;
import settingdust.mod_sets.util.SavingData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModSetsIngameConfig implements SavingData {
    private static final Gson GSON =
        new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(Component.class, ComponentAdapter.get().getTypeAdapter())
            .registerTypeAdapterFactory(ControllerRegistrar.SERIALIZER)
            .create();

    private static final Path modSetsPath = LoaderAdapter.get().getConfigDirectory().resolve("modsets.json");
    private static final Path rulesPath = LoaderAdapter.get().getConfigDirectory().resolve("rules");
    private static Map<String, ModSet> modSets = new HashMap<>();
    private static Multimap<String, ModSet> modSetsByMod = HashMultimap.create();
    private static Map<String, ModSet> definedModSets = new HashMap<>();
    private static Map<String, RuleSet> rules = new HashMap<>();

    public static Map<String, ModSet> modSets() {
        return modSets;
    }

    public static Map<String, RuleSet> rules() {
        return rules;
    }

    public static Multimap<String, ModSet> modSetsByMod() {
        return modSetsByMod;
    }

    public static ModSet getModSetOrThrow(String name) {
        var result = modSets.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No mod set named " + name);
        }
        return result;
    }

    @Override
    public void load() {
        try {Files.createDirectories(modSetsPath.getParent());} catch (IOException ignored) {}
        try {
            Files.createFile(modSetsPath);
            Files.writeString(modSetsPath, "{}");
        } catch (IOException ignored) {}
        try {
            definedModSets = GSON.fromJson(
                Files.newBufferedReader(modSetsPath),
                (TypeToken<Map<String, ModSet>>) TypeToken.getParameterized(Map.class, String.class, ModSet.class)
            );
        } catch (IOException e) {
            ModSets.LOGGER.error("Fail to load mod sets", e);
        }
        modSets.clear();
        modSets.putAll(definedModSets);
        ModSetLoadCallback.EVENT.getInvoker().onLoad();
        modSetsByMod.clear();
        for (var entry : modSets.entrySet()) {
            var modSet = entry.getValue();
            for (final var mod : modSet.mods()) {
                modSetsByMod.put(mod, modSet);
            }
        }

        try {Files.createDirectories(rulesPath);} catch (IOException ignored) {}
        try {
            for (final var rulePath : Files.newDirectoryStream(rulesPath, "*.json")) {
                try {
                    var rule = GSON.fromJson(Files.newBufferedReader(rulePath), TypeToken.get(RuleSet.class));
                    rules.put(FilenameUtils.getBaseName(rulePath.getFileName().toString()), rule);
                } catch (IOException e) {
                    ModSets.LOGGER.error("Fail to load rule {}", rulePath.getFileName(), e);
                }
            }
        } catch (IOException e) {
            ModSets.LOGGER.error("Fail to load rules", e);
        }
    }

    @Override
    public void save() {}

    @Override
    public boolean shouldLoad() {
        return LoaderAdapter.get().isInGame();
    }
}
