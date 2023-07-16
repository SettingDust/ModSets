package settingdust.modsets.quilt

import org.quiltmc.loader.api.LanguageAdapter
import org.quiltmc.loader.api.plugin.ModContainerExt
import org.quiltmc.loader.impl.QuiltLoaderImpl
import org.quiltmc.loader.impl.util.log.Log
import org.quiltmc.loader.impl.util.log.LogCategory
import settingdust.modsets.ModSets
import settingdust.modsets.config
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

private val logCategory = LogCategory.create("ModSets")

object ModSetsInjector {
    private val loader = QuiltLoaderImpl.INSTANCE

    @Suppress("UNCHECKED_CAST")
    private val modsProperty =
        QuiltLoaderImpl::class.memberProperties.single { it.name == "mods" } as KMutableProperty<MutableList<ModContainerExt>>

    private val mods: MutableList<ModContainerExt>

    init {
        try {
            requireNotNull(ModSets.config)
        } catch (e: Exception) {
            Log.error(logCategory, "ModSets config loading failed", e)
        }
        modsProperty.isAccessible = true
        mods = modsProperty.call(loader)
        hookSetupMods()
    }

    private object DummyList : MutableList<ModContainerExt> by mods {
        override fun iterator(): MutableIterator<ModContainerExt> {
            setupModsInvoked()
            return mods.iterator()
        }
    }

    private fun hookSetupMods() {
        modsProperty.setter.call(loader, DummyList)
    }

    private fun setupModsInvoked() {
        modsProperty.setter.call(loader, mods)
        // TODO Remove jar in jar mods
        mods.removeIf { it.metadata().id() in ModSets.config.disabledMods }
    }
}
