package settingdust.modsets

import java.nio.file.Path
import java.util.*

interface PlatformHelper {
    companion object :
        PlatformHelper by ServiceLoader.load(
            PlatformHelper::class.java, Companion::class.java.classLoader
        )
            .firstOrNull() ?: error("PlatformHelper implementation not found")

    val configDir: Path
}
