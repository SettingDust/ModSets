package settingdust.mod_sets.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import settingdust.mod_sets.game.ModSetsConfigScreenGenerator;

public class ModSetsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModSetsConfigScreenGenerator::generateScreen;
    }
}
