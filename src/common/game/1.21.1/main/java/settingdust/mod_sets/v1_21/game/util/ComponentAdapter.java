package settingdust.mod_sets.v1_21.game.util;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

import java.lang.reflect.Type;

public class ComponentAdapter
    implements settingdust.mod_sets.game.util.ComponentAdapter,
               JsonDeserializer<MutableComponent>,
               JsonSerializer<Component> {
    @Override
    public Object getTypeAdapter() {
        return this;
    }

    @Override
    public MutableComponent deserialize(
        final JsonElement jsonElement,
        final Type type,
        final JsonDeserializationContext jsonDeserializationContext
    ) throws JsonParseException {
        return (MutableComponent) ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(
            JsonParseException::new);
    }

    @Override
    public JsonElement serialize(
        final Component component,
        final Type type,
        final JsonSerializationContext jsonSerializationContext
    ) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component)
                                           .getOrThrow(JsonParseException::new);
    }
}
