package settingdust.mod_sets.v1_21.util

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import settingdust.mod_sets.util.serialization.ComponentSerializer

class ComponentSerializer : ComponentSerializer {
    init {
        requireNotNull(ComponentSerialization.CODEC)
    }

    override fun serialize(
        encoder: Encoder,
        value: Component
    ) {
        encoder.encodeSerializableValue(
            encoder.serializersModule.serializer<JsonElement>(),
            ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, value).orThrow,
        )
    }

    override fun deserialize(decoder: Decoder) = ComponentSerialization.CODEC.parse(
        JsonOps.INSTANCE,
        decoder.decodeSerializableValue(decoder.serializersModule.serializer<JsonElement>()),
    ).orThrow
}
