package settingdust.mod_sets.game.data.controller;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.OptionAddable;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.Rule;

import java.util.List;

public class RulesGroupControllerRegistrar implements ControllerRegistrar {
    private final List<Rule> rules;
    private boolean collapsed = true;

    public RulesGroupControllerRegistrar(final List<Rule> rules, final boolean collapsed) {
        this.rules = rules;
        this.collapsed = collapsed;
    }

    public RulesGroupControllerRegistrar(final List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public void registerCategory(final ConfigCategory.Builder category, final Rule rule) {
        var builder = OptionGroup.createBuilder()
                                 .name(rule.text())
                                 .collapsed(collapsed);
        var description = rule.description();
        if (description != null) {
            builder.description(OptionDescription.of(description));
        }
        for (final var nestedRule : rules) {
            nestedRule.controller().registerGroup(builder, nestedRule);
        }
        category.group(builder.build());
    }

    @Override
    public void registerGroup(final OptionGroup.Builder group, final Rule rule) {
        LabelControllerRegistrar.INSTANCE.registerGroup(group, rule);
        for (final var nestedRule : rules) {
            nestedRule.controller().registerGroup(group, nestedRule);
        }
    }

    @Override
    public void registerRule(final OptionAddable owner, final Rule rule) {
        // Doesn't directly register rule. Only groups.
    }
}
