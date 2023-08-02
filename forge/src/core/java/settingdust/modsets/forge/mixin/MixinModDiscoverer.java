package settingdust.modsets.forge.mixin;

import net.minecraftforge.fml.loading.moddiscovery.ModDiscoverer;
import net.minecraftforge.fml.loading.moddiscovery.ModValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import settingdust.modsets.ModSets;

@Mixin(value = ModDiscoverer.class, remap = false)
public class MixinModDiscoverer {
    @Inject(method = "discoverMods", at = @At("HEAD"))
    private void modsets$disableMods(CallbackInfoReturnable<ModValidator> cir) {
        ModSets.INSTANCE.getLogger().info("滴滴滴滴滴");
    }
}
