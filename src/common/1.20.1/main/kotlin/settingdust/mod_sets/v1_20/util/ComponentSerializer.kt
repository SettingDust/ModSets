package settingdust.mod_sets.v1_20.util

import com.google.gson.JsonElement
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import net.minecraft.network.chat.Component
import settingdust.mod_sets.util.serialization.ComponentSerializer

class ComponentSerializer : ComponentSerializer {
    init {
        Component.Serializer.toJson(Component.empty())
    }

    override fun serialize(encoder: Encoder, value: Component) {
        encoder.encodeSerializableValue(
            encoder.serializersModule.serializer<JsonElement>(),
            Component.Serializer.toJsonTree(value),
        )
    }

    override fun deserialize(decoder: Decoder) = Component.Serializer.fromJson(
        decoder.decodeSerializableValue(decoder.serializersModule.serializer<JsonElement>())
    )!!
}
