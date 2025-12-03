package settingdust.mod_sets.game.data;

import net.minecraft.network.chat.Component;

public record Rule(
    Component text,
    Component description,
    ControllerRegistrar controller
) {
}
