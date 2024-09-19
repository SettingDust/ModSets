package settingdust.modsets.quilt

import org.quiltmc.loader.api.LoaderValue
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin
import org.quiltmc.loader.api.plugin.QuiltPluginContext
import org.quiltmc.loader.api.plugin.solver.LoadOption
import org.quiltmc.loader.api.plugin.solver.ModLoadOption
import settingdust.modsets.ModSetsConfig

/**
 * Quilt not allow loader plugin for now
 *
 * org.quiltmc.json5.exception.ParseException: Mod mod-sets provides a loader plugin, which is not yet allowed!
 * 	at org.quiltmc.loader.impl.metadata.qmj.V1ModMetadataImpl.<init>(V1ModMetadataImpl.java:147)
 * 	at org.quiltmc.loader.impl.metadata.qmj.V1ModMetadataReader.readFields(V1ModMetadataReader.java:435)
 */

class ModSetsLoaderPlugin : QuiltLoaderPlugin {
    private lateinit var context: QuiltPluginContext

    override fun load(context: QuiltPluginContext, previousData: MutableMap<String, LoaderValue>) {
        this.context = context
    }

    override fun unload(data: MutableMap<String, LoaderValue>) {
    }

    override fun onLoadOptionAdded(option: LoadOption) {
        if (option !is ModLoadOption) return
        if (option.metadata().id() !in ModSetsConfig.disabledMods) return
        context.ruleContext().removeOption(option)
    }
}
