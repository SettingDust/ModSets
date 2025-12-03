package settingdust.mod_sets.forge;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

import java.util.List;
import java.util.Set;

public class ModSetsTransformationService implements ITransformationService {
    @Override
    public String name() {
        return "Mod Sets dummy";
    }

    @Override
    public void initialize(final IEnvironment environment) {

    }

    @Override
    public void onLoad(final IEnvironment env, final Set<String> otherServices) throws
                                                                                IncompatibleEnvironmentException {

    }

    @Override
    public List<ITransformer> transformers() {
        return List.of();
    }
}
