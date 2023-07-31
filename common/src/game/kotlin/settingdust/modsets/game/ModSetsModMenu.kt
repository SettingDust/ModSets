package settingdust.modsets.game

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

class ModSetsModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory {
        Rules.createScreen(it)
    }
}
