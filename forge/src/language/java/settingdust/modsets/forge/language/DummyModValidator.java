package settingdust.modsets.forge.language;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.loading.EarlyLoadingException;
import net.minecraftforge.fml.loading.moddiscovery.BackgroundScanHandler;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModValidator;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import settingdust.modsets.ConfigKt;
import settingdust.modsets.ModSets;
import settingdust.modsets.forge.platform.PlatformHelperForge;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DummyModValidator extends ModValidator {
    private static final Class<ModValidator> classModValidator = ModValidator.class;
    private static final Field fieldModFiles;

    private static final Field fieldBrokenFiles;

    private static final Field fieldDiscoveryErrorData;
    private static final Field fieldCandidateMods;

    private static final Class<ModFileInfo> classModFileInfo = ModFileInfo.class;
    private static final Field fieldMods;

    static {
        try {
            fieldModFiles = classModValidator.getDeclaredField("modFiles");
            fieldBrokenFiles = classModValidator.getDeclaredField("brokenFiles");
            fieldDiscoveryErrorData = classModValidator.getDeclaredField("discoveryErrorData");
            fieldCandidateMods = classModValidator.getDeclaredField("candidateMods");

            fieldModFiles.setAccessible(true);
            fieldBrokenFiles.setAccessible(true);
            fieldDiscoveryErrorData.setAccessible(true);
            fieldCandidateMods.setAccessible(true);

            fieldMods = classModFileInfo.getDeclaredField("mods");

            fieldMods.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final ModValidator validator;

    public DummyModValidator(ModValidator validator) throws IllegalAccessException {
        super(
                (Map<IModFile.Type, List<ModFile>>) fieldModFiles.get(validator),
                ((List<IModFile>) fieldBrokenFiles.get(validator))
                        .stream().map(IModFile::getModFileInfo).collect(Collectors.toList()),
                (List<EarlyLoadingException.ExceptionData>) fieldDiscoveryErrorData.get(validator));
        this.validator = validator;
    }

    @Override
    public BackgroundScanHandler stage2Validation() {
        new PlatformHelperForge().getConfigDir();
        Set<String> disabledMods = ConfigKt.getConfig(ModSets.INSTANCE).getDisabledMods();
        try {
            DummyModLanguageProvider.resetModValidator();
            final var candidateMods = (List<ModFile>) fieldCandidateMods.get(validator);
            final var toRemove = Lists.<ModFile>newArrayList();
            for (ModFile mod : candidateMods) {
                if (mod.getModFileInfo() instanceof ModFileInfo info) {
                    List<IModInfo> filtered = mod.getModInfos().stream()
                            .filter(it -> !disabledMods.contains(it.getModId()))
                            .toList();
                    if (filtered.isEmpty()) {
                        toRemove.add(mod);
                    } else fieldMods.set(info, filtered);
                }
            }
            candidateMods.removeAll(toRemove);
            return validator.stage2Validation();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
