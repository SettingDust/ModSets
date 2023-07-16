package settingdust.modsets.quilt

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import settingdust.modsets.ModSet
import settingdust.modsets.ModSets
import settingdust.modsets.rules

class Entrypoint : ModInitializer {
    override fun onInitialize(container: ModContainer) {
        // TODO The directories can't treat as mod set for now
        val modSets = ModSets.rules.modSets
        for (mod in QuiltLoader.getAllMods().map { it.metadata() }) {
            if (mod.id() in modSets) ModSets.logger.warn("Duplicate mod set with mod id: ${mod.id()}")
            val nameKey = "modmenu.nameTranslation.${mod.id()}"
            modSets.putIfAbsent(
                mod.id(),
                ModSet(
                    if (try {
                            I18n.exists(nameKey)
                        } catch (e: Exception) {
                            false
                        }
                    ) {
                        Component.translatable(nameKey)
                    } else {
                        Component.literal(mod.name())
                    },
                    Component.literal("${mod.id()}@${mod.version()}"),
                    listOf(mod.id()),
                ),
            )
        }
    }
}
