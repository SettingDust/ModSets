package settingdust.mod_sets.game.util;

import com.google.common.base.Suppliers;
import settingdust.mod_sets.util.ServiceLoaderUtil;

import java.util.function.Supplier;

public interface ComponentAdapter {
    Supplier<ComponentAdapter> supplier =
        Suppliers.memoize(() -> ServiceLoaderUtil.findService(ComponentAdapter.class));

    static ComponentAdapter get() {
        return supplier.get();
    }

    Object getTypeAdapter();
}
