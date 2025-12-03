package settingdust.mod_sets.game;

import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;

public class ModSetsConfigScreen extends YACLScreen {
    public ModSetsConfigScreen(final Screen screen) {
        super(ModSetsConfigScreenGenerator.generate(), screen);
    }
}
