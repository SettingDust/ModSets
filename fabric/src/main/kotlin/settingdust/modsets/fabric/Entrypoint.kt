package settingdust.modsets.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import settingdust.modsets.FilteredDirectoryModCandidateFinder
import settingdust.modsets.ModSet
import settingdust.modsets.ModSets
import settingdust.modsets.rules
import kotlin.io.path.div

object Entrypoint : ModInitializer {
    override fun onInitialize() {
        val gameDir = FabricLoaderImpl.INSTANCE.gameDir
        val modsPath = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
        for ((key, value) in FilteredDirectoryModCandidateFinder.directoryModSets.filterKeys { it !in ModSets.rules.modSets }
            .mapValues {
                val nameKey = "modmenu.nameTranslation.${it.key}"
                ModSet(
                    if (I18n.exists(nameKey)) Component.translatable(nameKey) else Component.literal(it.key),
                    Component.literal(gameDir.relativize(modsPath / it.key).toString()),
                    it.value.toList(),
                )
            }) {
            if (key in ModSets.rules.modSets) ModSets.logger.warn("Duplicate mod set with directory name: $key")
            ModSets.rules.modSets.putIfAbsent(key, value)
        }

        for (mod in FabricLoader.getInstance().allMods.map { it.metadata }) {
            if (mod.id in ModSets.rules.modSets) ModSets.logger.warn("Duplicate mod set with mod id: ${mod.id}")
            val nameKey = "modmenu.nameTranslation.${mod.id}"
            ModSets.rules.modSets.putIfAbsent(
                mod.id,
                ModSet(
                    if (try {
                            I18n.exists(nameKey)
                        } catch (e: Exception) {
                            false
                        }
                    ) {
                        Component.translatable(nameKey)
                    } else {
                        Component.literal(mod.name)
                    },
                    Component.literal("${mod.id}@${mod.version}"),
                    listOf(mod.id),
                ),
            )
        }
    }
}
