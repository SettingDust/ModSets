package settingdust.modsets.quilt

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.ModMetadata
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import settingdust.modsets.ModSet
import settingdust.modsets.ModSets
import settingdust.modsets.rules

class Entrypoint : ModInitializer {
    override fun onInitialize(container: ModContainer) {
        // TODO The directories can't treat as mod set for now
        val modSets = ModSets.rules.modSets
//        val entrypointKtClass = QuiltLoader::class.java.classLoader.loadClass("settingdust.modsets.quilt.EntrypointKt")
//        val modSetFunction = entrypointKtClass.getDeclaredMethod("ModSet", ModMetadata::class.java)
        for (mod in QuiltLoader.getAllMods().map { it.metadata() }) {
            if (mod.id() in modSets) ModSets.logger.warn("Duplicate mod set with mod id: ${mod.id()}")
            modSets.putIfAbsent(mod.id(), ModSet(mod))
        }
    }
}

fun ModSet(mod: ModMetadata) = ModSet(
    if (try {
            I18n.exists("modmenu.nameTranslation.${mod.id()}")
        } catch (e: Exception) {
            false
        }
    ) {
        Component.translatable("modmenu.nameTranslation.${mod.id()}")
    } else {
        Component.literal(mod.name())
    },
    Component.literal("${mod.id()}@${mod.version()}"),
    listOf(mod.id()),
)
