package settingdust.modsets.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import settingdust.modsets.ingame.ModSetsIngameConfig

class ModSetsModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory {
        ModSetsIngameConfig.generateConfigScreen(it)
    }
}
