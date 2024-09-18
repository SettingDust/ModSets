package settingdust.modsets

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.minecraft.util.GsonHelper
import org.quiltmc.qkl.library.serialization.annotation.CodecSerializable
import settingdust.kinecraft.serialization.unwrap
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.reader
import kotlin.io.path.writeText

object ModSetsConfig {
    @CodecSerializable
    data class Common(val badgeInModMenu: Boolean = true) {
        companion object {
            val CODEC = ModSets.CODEC_FACTORY.create<Common>()
        }
    }

    val GSON: Gson = GsonBuilder().create()

    private val commonPath = PlatformHelper.configDir / "common.json"
    var common = Common()
        private set

    private val disabledModsPath = PlatformHelper.configDir / "disabled_mods.json"
    var disabledMods: MutableSet<String> = mutableSetOf()
        private set
    private val disabledModsCodec = Codec.STRING.setOf()


    fun reload() {
        runCatching { PlatformHelper.configDir.createDirectories() }

        runCatching { disabledModsPath.createFile() }
        runCatching {
            disabledMods =
                disabledModsCodec
                    .parse(JsonOps.INSTANCE, GsonHelper.parse(commonPath.reader()))
                    .unwrap()
        }
        runCatching { commonPath.createFile() }
        runCatching {
            common =
                Common.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(commonPath.reader()))
                    .unwrap()
        }
        save()
    }

    fun save() {
        commonPath.writeText(
            Common.CODEC.encodeStart(JsonOps.INSTANCE, common).map { GSON.toJson(it) }.unwrap()
        )

        disabledModsPath.writeText(
            disabledModsCodec
                .encodeStart(JsonOps.INSTANCE, disabledMods)
                .map { GSON.toJson(it) }
                .unwrap())
    }
}
