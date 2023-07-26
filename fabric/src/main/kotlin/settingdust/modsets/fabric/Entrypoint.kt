package settingdust.modsets.fabric

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
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
        runBlocking(Dispatchers.IO) {
            ModSets.rules.ModSetsRegisterCallback.onEach {
                for ((key, value) in FilteredDirectoryModCandidateFinder.directoryModSets.filterKeys { it !in ModSets.rules.modSets }
                    .mapValues {
                        val nameKey = "modmenu.nameTranslation.${it.key}"
                        ModSet(
                            if (I18n.exists(nameKey)) Component.translatable(nameKey) else Component.literal(it.key),
                            Component.literal(gameDir.relativize(modsPath / it.key).toString()),
                            it.value,
                        )
                    }) {
                    if (key in ModSets.rules.modSets) ModSets.logger.warn("Duplicate mod set with directory name: $key")
                    ModSets.rules.modSets.putIfAbsent(key, value)
                }

                for (mod in FabricLoader.getInstance().allMods) {
                    if (mod.origin.kind.equals(ModOrigin.Kind.NESTED)) continue
                    val metadata = mod.metadata
                    if (metadata.type.equals("builtin")) continue
                    if (metadata.id in ModSets.rules.modSets) ModSets.logger.warn("Duplicate mod set with mod id: ${metadata.id}")
                    val nameKey = "modmenu.nameTranslation.${metadata.id}"
                    ModSets.rules.modSets.putIfAbsent(
                        metadata.id,
                        ModSet(
                            if (try {
                                    I18n.exists(nameKey)
                                } catch (e: Exception) {
                                    false
                                }
                            ) {
                                Component.translatable(nameKey)
                            } else {
                                Component.literal(metadata.name)
                            },
                            Component.literal("${metadata.id}@${metadata.version}"),
                            mutableListOf(metadata.id),
                        ),
                    )
                }
            }.collect {}
        }
    }
}
