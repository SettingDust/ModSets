package settingdust.modsets

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import org.slf4j.LoggerFactory

object ModSets {
    val logger by lazy { LoggerFactory.getLogger("ModSets")!! }
}

object ModSetsModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory {
        ModSets.rules.createScreen(it)
    }
}
