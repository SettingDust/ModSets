package settingdust.mod_sets.game.data.controller;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.controllers.LabelController;
import net.minecraft.network.chat.Component;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.Rule;

public class LabelControllerRegistrar implements ControllerRegistrar {
    public static final LabelControllerRegistrar INSTANCE = new LabelControllerRegistrar();

    @Override
    public void registerRule(final OptionAddable owner, final Rule rule) {
        var builder = Option.<Component>createBuilder()
                            .name(rule.text())
                            .customController(LabelController::new)
                            .stateManager(StateManager.createInstant(Binding.immutable(rule.text())));
        var description = rule.description();
        if (description != null) {
            builder.description(OptionDescription.of(description));
        }
        owner.option(builder.build());
    }
}
