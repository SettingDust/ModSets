package settingdust.mod_sets.game.data.controller;

import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionAddable;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.Rule;

public record BooleanControllerRegistrar(String id) implements ControllerRegistrar {
    public static Binding<Boolean> createBooleanBinding(final String id) {
        var mods = ModSetsIngameConfig.getModSetOrThrow(id).mods();
        return Binding.generic(
            true,
            () -> mods.stream().anyMatch(it -> !ModSetsDisabledMods.disabledMods().contains(it)),
            (newValue) -> {
                if (newValue) {
                    ModSetsDisabledMods.disabledMods().removeAll(mods);
                } else {
                    ModSetsDisabledMods.disabledMods().addAll(mods);
                }
            }
        );
    }

    @Override
    public void registerRule(final OptionAddable owner, final Rule rule) {
        var builder = Option.<Boolean>createBuilder()
                            .name(rule.text())
                            .controller(TickBoxControllerBuilder::create)
                            .stateManager(StateManager.createInstant(createBooleanBinding(id)));
        owner.option(ControllerRegistrar.assignDescription(builder, rule, id).build());
    }
}
