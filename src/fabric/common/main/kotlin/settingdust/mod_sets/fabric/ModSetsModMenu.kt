package settingdust.mod_sets.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import settingdust.mod_sets.game.ModSetsIngameConfig

class ModSetsModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory {
        ModSetsIngameConfig.generateConfigScreen(it)
    }
}
