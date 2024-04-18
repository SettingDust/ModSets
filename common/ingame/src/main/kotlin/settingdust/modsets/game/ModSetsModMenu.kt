package settingdust.modsets.game

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import settingdust.modsets.ModSets

class ModSetsModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory {
        ModSets.rules.createScreen(it)
    }
}
