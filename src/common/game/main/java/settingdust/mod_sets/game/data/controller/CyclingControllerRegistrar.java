package settingdust.mod_sets.game.data.controller;

import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionAddable;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import net.minecraft.network.chat.HoverEvent;
import org.apache.commons.lang3.ObjectUtils;
import settingdust.mod_sets.ModSets;
import settingdust.mod_sets.data.ModSetsDisabledMods;
import settingdust.mod_sets.game.ModSetsIngameConfig;
import settingdust.mod_sets.game.data.ControllerRegistrar;
import settingdust.mod_sets.game.data.Rule;
import settingdust.mod_sets.game.util.ExtraCollectors;

import java.util.List;

public class CyclingControllerRegistrar implements ControllerRegistrar {
    private final List<String> ids;
    private final String firstMod;

    public CyclingControllerRegistrar(final List<String> ids) {
        this.ids = ids;
        if (ids.isEmpty()) throw new IllegalArgumentException("CyclingControllerRegistrar must have at least one mod");
        //noinspection SequencedCollectionMethodCanBeUsed
        this.firstMod = ids.get(0);
    }

    @Override
    public void registerRule(final OptionAddable owner, final Rule rule) {
        var builder =
            Option
                .<String>createBuilder()
                .name(rule.text())
                .controller(opt ->
                    CyclingListControllerBuilder
                        .create(opt)
                        .values(ids)
                        .formatValue(id -> {
                            final var modSet = ModSetsIngameConfig.getModSetOrThrow(id);
                            return modSet.text()
                                         .copy()
                                         .withStyle(style -> style.withHoverEvent(new HoverEvent(
                                             HoverEvent.Action.SHOW_TEXT,
                                             modSet.description()
                                         )));
                        }))
                .stateManager(StateManager.createInstant(Binding.generic(
                    firstMod,
                    () -> {
                        var modSets = ModSetsIngameConfig.modSets();
                        var enabledModSets = ids.stream().filter(id -> {
                            var modSet = modSets.get(id);
                            if (modSet == null) return false;
                            var mods = modSet.mods();
                            return mods.stream().noneMatch(mod -> ModSetsDisabledMods.disabledMods()
                                                                                     .contains(mod));
                        }).toList();

                        if (enabledModSets.size() > 1) {
                            ModSets.LOGGER.warn(
                                "More than one mod set is enabled in cycling list: {}. Will take the " +
                                "first and disable the others",
                                String.join(", ", enabledModSets)
                            );
                            ModSetsDisabledMods
                                .disabledMods()
                                .addAll(
                                    enabledModSets
                                        .subList(1, enabledModSets.size())
                                        .stream()
                                        .flatMap(it ->
                                            ModSetsIngameConfig
                                                .getModSetOrThrow(it)
                                                .mods()
                                                .stream())
                                        .toList());
                            ModSetsDisabledMods
                                .disabledMods()
                                .removeAll(
                                    ModSetsIngameConfig
                                        .getModSetOrThrow(enabledModSets.get(0)).mods());
                        } else if (enabledModSets.isEmpty()) {
                            ModSets.LOGGER.warn(
                                "No mod set is enabled in cycling list: {}. Will take the first",
                                String.join(", ", ids)
                            );
                        }

                        var firstNonEmptyModSet = ids.stream()
                                                     .filter(it ->
                                                         !ModSetsIngameConfig
                                                             .getModSetOrThrow(it).mods().isEmpty())
                                                     .findFirst().orElse(null);
                        var enabledModSet =
                            enabledModSets
                                .stream()
                                .filter(id -> !ModSetsIngameConfig.getModSetOrThrow(id).isDisabled())
                                .collect(ExtraCollectors.singleOrNull());

                        var selected = ObjectUtils.firstNonNull(
                            enabledModSet,
                            firstNonEmptyModSet,
                            firstMod
                        );

                        ModSetsIngameConfig.getModSetOrThrow(selected).enableMods();

                        return selected;
                    }, id -> {

                    }
                )));
        owner.option(ControllerRegistrar.assignDescription(builder, rule, firstMod).build());
    }
}
