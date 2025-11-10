package settingdust.mod_sets.fabric.util.accessor

import net.fabricmc.loader.api.LanguageAdapter
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.lenni0451.reflect.stream.RStream

object FabricLoaderImplAccessor {
    private val clazz = FabricLoaderImpl::class.java

    private val stream = RStream.of(clazz)

    private val adapterMapField = stream.fields().by("adapterMap")

    val FabricLoaderImpl.adapterMap: MutableMap<String, LanguageAdapter>
        get() = adapterMapField.get(this)
}
