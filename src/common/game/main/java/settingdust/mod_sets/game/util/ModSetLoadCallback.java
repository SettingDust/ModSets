package settingdust.mod_sets.game.util;


import settingdust.mod_sets.util.Event;

import java.util.List;

public class ModSetLoadCallback {
    public static Event<Callback> EVENT = new Event<>((List<Callback> listeners) ->
        () -> {
            for (Callback listener : listeners) {
                listener.onLoad();
            }
        }
    );

    @FunctionalInterface
    public interface Callback {
        void onLoad();
    }
}
