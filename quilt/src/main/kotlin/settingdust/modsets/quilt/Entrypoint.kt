package settingdust.modsets.quilt

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.ModContainer.BasicSourceType
import org.quiltmc.loader.api.ModMetadata
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.loader.impl.QuiltLoaderImpl
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import settingdust.modsets.ModSets
import settingdust.modsets.ModSetsConfig
import settingdust.modsets.ingame.ModSet
import settingdust.modsets.ingame.ModSetsIngameConfig
import kotlin.io.path.name

class Entrypoint : ModInitializer {
    override fun onInitialize(container: ModContainer) {
        val modSets = ModSetsIngameConfig.modSets
        val modDir = QuiltLoaderImpl.INSTANCE.modsDir

        GlobalScope.launch {
            ModSetsIngameConfig.MOD_SET_REGISTER_CALLBACK.collect {
                for (mod in QuiltLoader.getAllMods()) {
                    if (mod.sourceType.equals(BasicSourceType.BUILTIN)) continue
                    val metadata = mod.metadata()
                    // I can't find the real case of muleiple quilt source paths. So, just use the first
                    val paths = mod.sourcePaths.singleOrNull() ?: continue
                    // Quilt will be writing the path like [path in system, path in jar] etc.
                    val pathInSystem = paths.singleOrNull() ?: continue
                    if (pathInSystem.startsWith(modDir) && modDir != pathInSystem.parent) {
                        val subDir = pathInSystem.parent.name
                        ModSets.LOGGER.debug("Add {} to {}", pathInSystem, subDir)
                        if (subDir in modSets)
                            ModSets.LOGGER.warn("Duplicate mod set with directory name: $subDir")
                        modSets
                            .getOrPut(subDir) {
                                ModSet(
                                    Component.literal(subDir),
                                    Component.literal(pathInSystem.toString()),
                                    mutableSetOf()
                                )
                            }
                            .mods
                            .add(metadata.id())
                    }
                    if (metadata.id() in modSets)
                        ModSets.LOGGER.warn("Duplicate mod set with mod id: ${metadata.id()}")
                    modSets.putIfAbsent(metadata.id(), ModSet(metadata))
                }

                ModSetsConfig.initialDisabledMods.forEach {
                    modSets.putIfAbsent(
                        it,
                        ModSet(
                            Component.literal(it),
                            if (
                                try {
                                    I18n.exists("modmenu.nameTranslation.$it")
                                } catch (e: Exception) {
                                    false
                                }
                            ) {
                                Component.translatable("modmenu.nameTranslation.$it")
                            } else {
                                Component.literal(it)
                            }
                                .append(" ")
                                .append(Component.literal("$it@disabled")),
                            mutableSetOf(it),
                        )
                    )
                }
            }
        }
    }
}

fun ModSet(mod: ModMetadata) =
    ModSet(
        Component.literal(mod.id()),
        if (
            try {
                I18n.exists("modmenu.nameTranslation.${mod.id()}")
            } catch (e: Exception) {
                false
            }
        ) {
            Component.translatable("modmenu.nameTranslation.${mod.id()}")
        } else {
            Component.literal(mod.name())
        }
            .append(" ")
            .append(Component.literal("${mod.id()}@${mod.version()}")),
        mutableSetOf(mod.id()),
    )
