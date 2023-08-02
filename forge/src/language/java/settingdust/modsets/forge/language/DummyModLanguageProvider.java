package settingdust.modsets.forge.language;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModValidator;
import net.minecraftforge.forgespi.language.ILifecycleEvent;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DummyModLanguageProvider implements IModLanguageProvider {
    private static final Class<FMLLoader> loaderClass = FMLLoader.class;
    private static final Field modValidator;
    private static ModValidator originalValidator;

    static {
        try {
            modValidator = loaderClass.getDeclaredField("modValidator");
            modValidator.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public DummyModLanguageProvider() {
        hookModValidator();
    }

    public static void hookModValidator() {
        try {
            originalValidator = (ModValidator) modValidator.get(null);
            modValidator.set(null, new DummyModValidator(originalValidator));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void resetModValidator() throws IllegalAccessException {
        modValidator.set(null, originalValidator);
    }

    @Override
    public String name() {
        return "mod sets dummy";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return (data) -> {
        };
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> consumeEvent) {

    }
}
