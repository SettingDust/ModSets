package settingdust.modsets.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import settingdust.modsets.ModSets
import settingdust.modsets.config
import settingdust.modsets.game.ModSet
import settingdust.modsets.game.Rules.ModSetsRegisterCallbacks
import settingdust.modsets.game.rules
import kotlin.io.path.div

object Entrypoint : ModInitializer {
    override fun onInitialize() {
        val gameDir = FabricLoaderImpl.INSTANCE.gameDir
        val modsPath = FabricLoaderImpl.INSTANCE.modsDirectory.toPath()
        val modSets = ModSets.rules.modSets

        ModSetsRegisterCallbacks += {
            for ((key, value) in FilteredDirectoryModCandidateFinder.directoryModSets
                .mapValues {
                    ModSet(
                        Component.literal(it.key),
                        Component.literal(gameDir.relativize(modsPath / it.key).toString()),
                        it.value.toMutableSet(),
                    )
                }) {
                if (key in modSets) ModSets.logger.warn("Duplicate mod set with directory name: $key")
                modSets.putIfAbsent(key, value)
            }

            for (mod in FabricLoader.getInstance().allMods) {
                if (mod.origin.kind.equals(ModOrigin.Kind.NESTED)) continue
                val metadata = mod.metadata
                if (metadata.type.equals("builtin")) continue
                if (metadata.id in modSets) ModSets.logger.warn("Duplicate mod set with mod id: ${metadata.id}")
                val nameKey = "modmenu.nameTranslation.${metadata.id}"
                modSets.putIfAbsent(
                    metadata.id,
                    ModSet(
                        Component.literal(metadata.id),
                        if (try {
                                I18n.exists(nameKey)
                            } catch (e: Exception) {
                                false
                            }
                        ) {
                            Component.translatable(nameKey)
                        } else {
                            Component.literal(metadata.name)
                        }.append(" ").append(Component.literal("${metadata.id}@${metadata.version}")),
                        mutableSetOf(metadata.id),
                    ),
                )
            }

            ModSets.config.disabledMods.forEach {
                modSets.putIfAbsent(
                    it, ModSet(
                        Component.literal(it),
                        if (try {
                                I18n.exists("modmenu.nameTranslation.$it")
                            } catch (e: Exception) {
                                false
                            }
                        ) {
                            Component.translatable("modmenu.nameTranslation.$it")
                        } else {
                            Component.literal(it)
                        }.append(" ").append(Component.literal("$it@disabled")),
                        mutableSetOf(it),
                    )
                )
            }
        }
    }
}
