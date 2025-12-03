package settingdust.mod_sets.game.data;

import net.minecraft.network.chat.Component;
import settingdust.mod_sets.data.ModSetsDisabledMods;

import java.util.Set;

public record ModSet(
    Component text,
    Component description,
    Set<String> mods
) {
    public boolean isDisabled() {
        return mods().stream().anyMatch(mod -> ModSetsDisabledMods.disabledMods().contains(mod));
    }

    public void disableMods() {
        ModSetsDisabledMods.disabledMods().addAll(mods());
    }

    public void enableMods() {
        ModSetsDisabledMods.disabledMods().removeAll(mods());
    }
}
