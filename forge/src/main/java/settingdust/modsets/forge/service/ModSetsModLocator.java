package settingdust.modsets.forge.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.ModFileLoadingException;
import net.minecraftforge.jarjar.selection.JarSelector;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Inspired by
// https://github.com/Chaos02/SubFolderLoader/blob/main/src/main/java/com/chaos02/structuredmodloader/StructuredModLoader.java
public class ModSetsModLocator extends AbstractJarFileModLocator {
    static final Attributes.Name TYPE = new Attributes.Name("FMLModType");
    private static final Logger logger = LoggerFactory.getLogger(ModSetsModLocator.class);
    public static Map<String, List<String>> directoryModSet = new HashMap<>();

    @Override
    protected ModFileOrException createMod(Path... path) {
        final var result = super.createMod(path);
        if (result.file() == null) return result;
        final var filePath = result.file().getFilePath();
        if (filePath.equals(FMLPaths.MODSDIR.get())) return result;
        final var parent = filePath.getParent();
        if (parent == null) return result;
        final var dirName = filePath.getParent().getFileName().toString();
        directoryModSet.putIfAbsent(dirName, new ArrayList<>());
        directoryModSet
                .get(dirName)
                .addAll(result.file().getModInfos().stream()
                        .map(IModInfo::getModId)
                        .toList());
        return super.createMod(path);
    }

    @Override
    public List<ModFileOrException> scanMods() {
        final var result = Lists.newArrayList(super.scanMods());
        String path = null;
        try {
            path = getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            path = path.substring(1, path.lastIndexOf("/"));
            if (path.lastIndexOf("#") != -1) path = path.substring(0, path.lastIndexOf("#"));
            final List<IModFile> dependenciesToLoad = JarSelector.detectAndSelect(
                    Lists.newArrayList(createMod(Paths.get(path)).file()),
                    this::loadResourceFromModFile,
                    this::loadModFileFrom,
                    this::identifyMod,
                    this::exception);
            result.addAll(dependenciesToLoad.stream()
                    .map(it -> new ModFileOrException(it, null))
                    .toList());
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<Path> scanCandidates() {
        var modsDir = FMLPaths.GAMEDIR.get().resolve(FMLPaths.MODSDIR.get());
        try (var dirs = Files.list(modsDir).filter(Files::isDirectory)) {
            var dirList = dirs.toList();
            logger.info("Loading mods from {} sub dir in mods", dirList.size());
            logger.debug(String.join(
                    ",", dirList.stream().map(it -> it.getFileName().toString()).toList()));

            return dirList.stream().flatMap(it -> {
                try {
                    return Streams.stream(Files.newDirectoryStream(it, "*.jar"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "mod sets";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {}

    protected Optional<InputStream> loadResourceFromModFile(final IModFile modFile, final Path path) {
        try {
            return Optional.of(Files.newInputStream(modFile.findResource(path.toString())));
        } catch (final FileNotFoundException e) {
            logger.debug(
                    "Failed to load resource {} from {}, it does not contain dependency information.",
                    path,
                    modFile.getFileName());
            return Optional.empty();
        } catch (final Exception e) {
            logger.error("Failed to load resource {} from mod {}, cause {}", path, modFile.getFileName(), e);
            return Optional.empty();
        }
    }

    protected Optional<IModFile> loadModFileFrom(final IModFile file, final Path path) {
        try {
            final Path pathInModFile = file.findResource(path.toString());
            final URI filePathUri =
                    new URI("jij:" + (pathInModFile.toAbsolutePath().toUri().getRawSchemeSpecificPart())).normalize();
            final Map<String, ?> outerFsArgs = ImmutableMap.of("packagePath", pathInModFile);
            final FileSystem zipFS = FileSystems.newFileSystem(filePathUri, outerFsArgs);
            final Path pathInFS = zipFS.getPath("/");
            return Optional.of(createMod(pathInFS).file());
        } catch (Exception e) {
            logger.error("Failed to load mod file {} from {}", path, file.getFileName());
            final RuntimeException exception =
                    new ModFileLoadingException("Failed to load mod file " + file.getFileName());
            exception.initCause(e);

            throw exception;
        }
    }

    protected String identifyMod(final IModFile modFile) {
        if (modFile.getModFileInfo() == null || modFile.getModInfos().isEmpty()) {
            return modFile.getFileName();
        }

        return modFile.getModInfos().stream().map(IModInfo::getModId).collect(Collectors.joining());
    }

    protected EarlyLoadingException exception(
            Collection<JarSelector.ResolutionFailureInformation<IModFile>> failedDependencies) {

        final List<EarlyLoadingException.ExceptionData> errors = failedDependencies.stream()
                .filter(entry -> !entry.sources().isEmpty()) // Should never be the case, but just to be sure
                .map(this::buildExceptionData)
                .toList();

        return new EarlyLoadingException(
                failedDependencies.size() + " Dependency restrictions were not met.", null, errors);
    }

    @NotNull
    private EarlyLoadingException.ExceptionData buildExceptionData(
            final JarSelector.ResolutionFailureInformation<IModFile> entry) {
        return new EarlyLoadingException.ExceptionData(
                getErrorTranslationKey(entry),
                entry.identifier().group() + ":" + entry.identifier().artifact(),
                entry.sources().stream()
                        .flatMap(this::getModWithVersionRangeStream)
                        .map(this::formatError)
                        .collect(Collectors.joining(", ")));
    }

    private String getErrorTranslationKey(final JarSelector.ResolutionFailureInformation<IModFile> entry) {
        return entry.failureReason() == JarSelector.FailureReason.VERSION_RESOLUTION_FAILED
                ? "fml.dependencyloading.conflictingdependencies"
                : "fml.dependencyloading.mismatchedcontaineddependencies";
    }

    @NotNull
    private Stream<ModWithVersionRange> getModWithVersionRangeStream(
            final JarSelector.SourceWithRequestedVersionRange<IModFile> file) {
        return file.sources().stream()
                .map(IModFile::getModFileInfo)
                .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                .map(modInfo -> new ModWithVersionRange(modInfo, file.requestedVersionRange(), file.includedVersion()));
    }

    @NotNull
    private String formatError(final ModWithVersionRange modWithVersionRange) {
        return "\u00a7e" + modWithVersionRange.modInfo().getModId() + "\u00a7r - \u00a74"
                + modWithVersionRange.versionRange().toString() + "\u00a74 - \u00a72"
                + modWithVersionRange.artifactVersion().toString() + "\u00a72";
    }

    private record ModWithVersionRange(IModInfo modInfo, VersionRange versionRange, ArtifactVersion artifactVersion) {}
}
