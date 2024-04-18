package settingdust.modsets

import java.nio.file.Path
import java.util.*

interface PlatformHelper {
    companion object {
        @JvmStatic
        val INSTANCE =
            ServiceLoader.load(PlatformHelper::class.java, Companion::class.java.classLoader)
                .first()!!

        val configDir: Path
            get() = INSTANCE.configDir
    }

    val configDir: Path
}
