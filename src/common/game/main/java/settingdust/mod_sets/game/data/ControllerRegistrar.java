package settingdust.mod_sets.game.data;

import dev.isxander.yacl3.api.*;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.controller.*;
import settingdust.mod_sets.game.util.RuntimeTypeAdapterFactory;

public interface ControllerRegistrar {
    RuntimeTypeAdapterFactory<ControllerRegistrar> SERIALIZER =
        RuntimeTypeAdapterFactory
            .of(ControllerRegistrar.class, "type")
            .registerSubtype(ModsGroupControllerRegistrar.class, "mods_group")
            .registerSubtype(RulesGroupControllerRegistrar.class, "rules_group")
            .registerSubtype(BooleanControllerRegistrar.class, "boolean")
            .registerSubtype(LabelControllerRegistrar.class, "label")
            .registerSubtype(CyclingControllerRegistrar.class, "cycling");

    static <T> Option.Builder<T> assignDescription(Option.Builder<T> builder, Rule rule, String id) {
        var description = rule.description();
        if (description == null) {
            var modSet = ModSetsIngameConfig.modSets().get(id);
            if (modSet != null) {
                description = modSet.description();
            }
        }
        if (description != null) {
            builder.description(OptionDescription.of(description));
        }
        return builder;
    }

    default void registerCategory(ConfigCategory.Builder category, Rule rule) {
        registerRule(category, rule);
    }

    default void registerGroup(OptionGroup.Builder group, Rule rule) {
        registerRule(group, rule);
    }

    void registerRule(OptionAddable owner, Rule rule);
}
