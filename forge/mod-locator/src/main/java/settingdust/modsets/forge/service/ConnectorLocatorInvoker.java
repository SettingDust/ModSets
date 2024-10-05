package settingdust.modsets.forge.service;

import net.minecraftforge.fml.loading.StringUtils;
import org.sinytra.connector.loader.ConnectorLoaderModMetadata;
import org.sinytra.connector.locator.ConnectorLocator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

public class ConnectorLocatorInvoker {
    public static final boolean CONNECTOR_EXIST;

    private static final Class<ConnectorLocator> locatorClass = ConnectorLocator.class;
    private static Method isFabricModJarMethod;
    private static Method shouldIgnoreModMethod;

    static {
        boolean exist;
        try {
            Class.forName("org.sinytra.connector.locator.ConnectorLocator");
            exist = true;

            try {
                isFabricModJarMethod = ConnectorLocator.class.getDeclaredMethod("isFabricModJar", Path.class);
                shouldIgnoreModMethod = ConnectorLocator.class.getDeclaredMethod(
                    "shouldIgnoreMod",
                    ConnectorLoaderModMetadata.class,
                    Collection.class
                );
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } catch (ClassNotFoundException e) {
            exist = false;
        }

        CONNECTOR_EXIST = exist;
    }

    public static Stream<Path> filterFabricJars(Stream<Path> stream) {
        return stream
            .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(".jar"))
            .sorted(Comparator.comparing(path -> StringUtils.toLowerCase(path.getFileName().toString())))
            .filter(ConnectorLocatorInvoker::isFabricModJar);
    }

    public static boolean shouldIgnoreMod(ConnectorLoaderModMetadata metadata, Collection<String> modIds) {
        try {
            return (boolean) shouldIgnoreModMethod.invoke(null, metadata, modIds);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isFabricModJar(Path path) {
        try {
            return (boolean) isFabricModJarMethod.invoke(null, path);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
