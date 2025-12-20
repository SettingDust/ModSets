package settingdust.mod_sets.util;

import com.google.common.base.Suppliers;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Supplier;

public interface LoaderAdapter {
    Supplier<LoaderAdapter> supplier =
        Suppliers.memoize(() -> ServiceLoaderUtil.findService(
            LoaderAdapter.class,
            ServiceLoader.load(LoaderAdapter.class, LoaderAdapter.class.getClassLoader())
        ));

    static LoaderAdapter get() {
        return supplier.get();
    }

    boolean isClient();

    boolean isModLoaded(String modId);

    Path getConfigDirectory();

    Path getModsDirectory();

    boolean isInGame();
}
