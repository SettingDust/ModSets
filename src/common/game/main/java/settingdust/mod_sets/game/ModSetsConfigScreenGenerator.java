package settingdust.mod_sets.game;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import settingdust.mod_sets.ModSets;

import java.util.Collection;
import java.util.HashSet;

public class ModSetsConfigScreenGenerator {
    private static void save() {
        ModSets.save();
    }

    public static YetAnotherConfigLib generate() {
        ModSets.load();
        var builder =
            YetAnotherConfigLib
                .createBuilder()
                .title(Component.translatable("modsets.name"))
                .save(ModSetsConfigScreenGenerator::save);

        var noRule = ModSetsIngameConfig.rules().isEmpty();
        if (noRule) {
            builder.category(
                ConfigCategory
                    .createBuilder()
                    .name(Component.translatable("modsets.no_rules"))
                    .build());
            return builder.build();
        }

        var options = new HashSet<Option<Object>>();
        for (final var ruleSetEntry : ModSetsIngameConfig.rules().entrySet()) {
            var ruleSet = ruleSetEntry.getValue();
            var categoryBuilder =
                ConfigCategory
                    .createBuilder()
                    .name(ruleSet.text());
            var description = ruleSet.description();
            if (description != null) {
                categoryBuilder.tooltip(description);
            }
            for (final var rule : ruleSet.rules()) {
                rule.controller().registerCategory(categoryBuilder, rule);
            }
            var category = categoryBuilder.build();
            for (final var group : category.groups()) {
                //noinspection unchecked
                options.addAll((Collection<Option<Object>>) group.options());
            }
            builder.category(category);
        }

        for (final var option : options) {
            option.addEventListener((currentOption, event) -> {
                var changed = false;
                for (final var anotherOption : options) {
                    if (anotherOption == currentOption ||
                        anotherOption.pendingValue() == anotherOption.binding().getValue()) continue;
                    anotherOption.requestSet(anotherOption.stateManager().get());
                    if (changed || currentOption.pendingValue() == currentOption.binding().getValue()) continue;
                    changed = true;
                    ModSets.LOGGER.warn(
                        "Option {} change is conflicting with option {}. Can't apply.",
                        currentOption.name(),
                        anotherOption.name()
                    );
                }
                if (currentOption.pendingValue() != currentOption.binding().getValue()) {
                    ModSets.LOGGER.warn(
                        "Option {} change is conflicting with unknown option. Can't apply.",
                        currentOption.name()
                    );
                    currentOption.requestSet(currentOption.binding().getValue());
                }
                currentOption.applyValue();
                save();
            });
        }

        return builder.build();
    }

    public static Screen generateScreen(Screen parent) {
        return new ModSetsConfigScreen(parent);
    }
}
