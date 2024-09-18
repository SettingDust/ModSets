package settingdust.modsets.mixin.fabric;

import com.terraformersmc.modmenu.util.mod.Mod;
import com.terraformersmc.modmenu.util.mod.ModBadgeRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import settingdust.modsets.ModSetsConfig;
import settingdust.modsets.game.ModSet;
import settingdust.modsets.game.ModSetsIngameConfig;

@Mixin(value = ModBadgeRenderer.class, remap = false)
public abstract class ModBadgeRendererMixin {

    @Shadow
    protected Mod mod;

    @Shadow
    public abstract void drawBadge(
        final GuiGraphics par1,
        final FormattedCharSequence par2,
        final int par3,
        final int par4,
        final int par5,
        final int par6
    );

    @Inject(method = "draw", at = @At("TAIL"))
    public void drawCustomBadges(
        final GuiGraphics guiGraphics,
        final int mouseX,
        final int mouseY,
        final CallbackInfo ci
    ) {
        if (!ModSetsConfig.INSTANCE.getCommon().getBadgeInModMenu()) return;
        try {
            for (final ModSet modSet : ModSetsIngameConfig.INSTANCE.getModIdToModSets().get(mod.getId())) {
                drawBadge(
                    guiGraphics,
                    modSet.getText().getVisualOrderText(),
                    Mod.Badge.MODPACK.getOutlineColor(),
                    Mod.Badge.MODPACK.getFillColor(),
                    mouseX,
                    mouseY
                );
            }
        } catch (Exception ignored) {
        }
    }
}
