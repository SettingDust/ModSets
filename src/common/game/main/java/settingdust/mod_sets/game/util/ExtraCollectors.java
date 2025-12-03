package settingdust.mod_sets.game.util;

import java.util.stream.Collector;

public class ExtraCollectors {
    public static <T> Collector<T, ?, T> singleOrNull() {
        class Box {
            T value;
            boolean tooMany = false;
        }

        return Collector.of(
            Box::new,
            (box, elem) -> {
                if (box.value == null) {
                    box.value = elem;
                } else {
                    box.tooMany = true;
                }
            },
            (b1, b2) -> {
                if (b1.tooMany || b2.tooMany) {
                    b1.tooMany = true;
                    b1.value = null;
                } else {
                    if (b1.value == null) b1.value = b2.value;
                    else b1.tooMany = true;
                }
                return b1;
            },
            box -> (box.tooMany ? null : box.value)
        );
    }
}
