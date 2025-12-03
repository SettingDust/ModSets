package settingdust.mod_sets.game.data.controller;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.ModSet;
import settingdust.mod_sets.game.data.Rule;

import java.util.List;

public class ModsGroupControllerRegistrar implements ControllerRegistrar {
    private final List<String> ids;
    private boolean collapsed = true;
    private boolean showMods = true;

    public ModsGroupControllerRegistrar(final List<String> ids, final boolean collapsed, final boolean showMods) {
        this.ids = ids;
        this.collapsed = collapsed;
        this.showMods = showMods;

        if (ids.isEmpty())
            throw new IllegalArgumentException("ModsGroupControllerRegistrar must have at least one mod");
    }

    public ModsGroupControllerRegistrar(final List<String> ids) {
        this.ids = ids;
    }

    private static Option.Builder<Boolean> modSetToOption(final String id, final ModSet modSet) {
        var builder =
            Option.<Boolean>createBuilder()
                  .name(modSet.text())
                  .controller(TickBoxControllerBuilder::create)
                  .stateManager(StateManager.createInstant(BooleanControllerRegistrar.createBooleanBinding(id)));
        var description = modSet.description();
        if (description != null) {
            builder.description(OptionDescription.of(description));
        }
        return builder;
    }

    @Override
    public void registerCategory(final ConfigCategory.Builder category, final Rule rule) {
        var builder = OptionGroup.createBuilder();
        builder.name(rule.text()).collapsed(collapsed);
        var description = rule.description();
        if (description != null) {
            builder.description(OptionDescription.of(description));
        }
        registerRule(builder, rule);
        category.group(builder.build());
    }

    @Override
    public void registerGroup(final OptionGroup.Builder group, final Rule rule) {
        LabelControllerRegistrar.INSTANCE.registerRule(group, rule);
        ControllerRegistrar.super.registerGroup(group, rule);
    }

    @Override
    public void registerRule(final OptionAddable owner, final Rule rule) {
        for (final var id : ids) {
            var modSet = ModSetsIngameConfig.modSets().get(id);
            if (modSet == null) continue;
            owner.option(modSetToOption(id, modSet).build());

            if (showMods) {
                for (final var nestedId : modSet.mods()) {
                    if (nestedId.equals(id)) continue;
                    var nestedModSet = ModSetsIngameConfig.modSets().get(nestedId);
                    if (nestedModSet == null) continue;
                    owner.option(modSetToOption(nestedId, nestedModSet).build());
                }
            }
        }
    }
}
