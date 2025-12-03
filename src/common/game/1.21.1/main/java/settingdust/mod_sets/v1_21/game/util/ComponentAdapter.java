package settingdust.mod_sets.v1_21.game.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

public class ComponentAdapter implements settingdust.mod_sets.game.util.ComponentAdapter {
    private static final Component.SerializerAdapter SERIALIZER = new Component.SerializerAdapter(RegistryAccess.EMPTY);

    @Override
    public Object getTypeAdapter() {
        return SERIALIZER;
    }
}
