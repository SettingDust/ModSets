package settingdust.mod_sets.v1_20.game.util;

import net.minecraft.network.chat.Component;

public class ComponentAdapter implements settingdust.mod_sets.game.util.ComponentAdapter {
    private static final Component.Serializer SERIALIZER = new Component.Serializer();

    @Override
    public Object getTypeAdapter() {
        return SERIALIZER;
    }
}
