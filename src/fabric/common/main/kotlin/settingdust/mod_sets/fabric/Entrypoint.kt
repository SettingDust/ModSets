package settingdust.mod_sets.fabric

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModOrigin
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import settingdust.mod_sets.ModSets
import settingdust.mod_sets.data.ModSetsDisabledMods
import settingdust.mod_sets.ingame.ModSet
import settingdust.mod_sets.ingame.ModSetsIngameConfig
import settingdust.mod_sets.ingame.util.ModSetLoadCallback
import settingdust.mod_sets.util.Entrypoint

object ModSetsFabric {
    init {
        requireNotNull(ModSets)
        Entrypoint.construct()
    }

    fun init() {
        ModSetLoadCallback.CALLBACK.register {
            val modSets = ModSetsIngameConfig.modSets

            for (mod in FabricLoader.getInstance().allMods) {
                if (mod.origin.kind.equals(ModOrigin.Kind.NESTED)) continue
                val metadata = mod.metadata
                if (metadata.type.equals("builtin")) continue
                if (metadata.id in modSets)
                    ModSets.LOGGER.warn("Duplicate mod set with mod id: ${metadata.id}")
                val nameKey = "modmenu.nameTranslation.${metadata.id}"
                modSets.putIfAbsent(
                    metadata.id,
                    ModSet(
                        Component.literal(metadata.id),
                        if (
                            try {
                                I18n.exists(nameKey)
                            } catch (_: Exception) {
                                false
                            }
                        ) {
                            Component.translatable(nameKey)
                        } else {
                            Component.literal(metadata.name)
                        }
                            .append(" ")
                            .append(Component.literal("${metadata.id}@${metadata.version}")),
                        mutableSetOf(metadata.id),
                    ),
                )
            }

            ModSetsDisabledMods.definedModSets.forEach {
                modSets.putIfAbsent(
                    it,
                    ModSet(
                        Component.literal(it),
                        if (
                            try {
                                I18n.exists("modmenu.nameTranslation.$it")
                            } catch (_: Exception) {
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
        Entrypoint.init()
    }

    fun clientInit() {
        Entrypoint.clientInit()
    }
}
