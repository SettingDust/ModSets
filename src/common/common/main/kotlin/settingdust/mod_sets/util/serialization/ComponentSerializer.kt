package settingdust.mod_sets.util.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonElement
import net.minecraft.network.chat.Component
import settingdust.mod_sets.util.ServiceLoaderUtil

interface ComponentSerializer : KSerializer<Component> {
    companion object : ComponentSerializer by ServiceLoaderUtil.findService<ComponentSerializer>() {
        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor = SerialDescriptor(
            "net.minecraft.network.chat.Component",
            JsonElement.serializer().descriptor,
        )
    }

    override val descriptor: SerialDescriptor
        get() = Companion.descriptor
}
