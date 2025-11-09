package settingdust.mod_sets.util

import java.nio.file.Path

interface LoaderAdapter {
    companion object : LoaderAdapter by ServiceLoaderUtil.findService()

    val isClient: Boolean

    fun isModLoaded(modId: String): Boolean

    val configPath: Path
}
