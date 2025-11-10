package settingdust.mod_sets.fabric.util;

import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import settingdust.mod_sets.data.ModSetsDisabledMods;

import java.util.Collection;

/**
 * Kotlin can't resolve reference of {@link ModCandidateConsumer}
 */
public class ModContainerModCandidateFinder extends ClasspathModCandidateFinder {
    private final Collection<ModContainerImpl> containers;

    public ModContainerModCandidateFinder(Collection<ModContainerImpl> containers) {
        this.containers = containers;
    }

    @Override
    public void findCandidates(ModCandidateConsumer out) {
        containers.forEach((ModContainerImpl container) -> {
            if (ModSetsDisabledMods.Companion.getDisabledMods().contains(container.getMetadata().getId()))
                return;
            // Nested are added in ModResolver#resolve
            if (container.getOrigin().getKind().equals(ModOrigin.Kind.PATH))
                out.accept(container.getOrigin().getPaths(), false);
        });
    }
}
