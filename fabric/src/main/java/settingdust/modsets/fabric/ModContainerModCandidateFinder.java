package settingdust.modsets.fabric;

import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import settingdust.modsets.ConfigKt;
import settingdust.modsets.ModSets;

import java.util.List;

/**
 * Kotlin can't resolve reference of {@link ModCandidateConsumer}
 */
public class ModContainerModCandidateFinder extends ClasspathModCandidateFinder {
    private final List<ModContainerImpl> containers;

    public ModContainerModCandidateFinder(List<ModContainerImpl> containers) {
        this.containers = containers;
    }

    @Override
    public void findCandidates(ModCandidateConsumer out) {
        containers.forEach((ModContainerImpl container) -> {
            if (ConfigKt.getConfig(ModSets.INSTANCE).getDisabledMods().contains(container.getMetadata().getId()))
                return;
            // Nested are added in ModResolver#resolve
            if (container.getOrigin().getKind().equals(ModOrigin.Kind.PATH))
                out.accept(container.getOrigin().getPaths(), false);
        });
    }
}
