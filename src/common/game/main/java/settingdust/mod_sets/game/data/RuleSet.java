package settingdust.mod_sets.game.data;

import net.minecraft.network.chat.Component;

import java.util.List;

public record RuleSet(
    Component text,
    Component description,
    List<Rule> rules
) {
}
