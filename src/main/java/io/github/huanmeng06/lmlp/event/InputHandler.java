package io.github.huanmeng06.lmlp.event;

import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Hotkeys;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    // True while the first key of the open-config combo (the vanilla advancements
    // key) is held in-game with its vanilla action suppressed, waiting to see
    // whether the rest of the combo follows.
    private boolean comboFirstKeyHeld;

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    private InputHandler() {
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (var hotkey : Hotkeys.HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                LitematicaMaterialListPlus.MOD_NAME,
                "lmlp.hotkeys.category.material_list",
                Hotkeys.HOTKEY_LIST
        );
    }

    /**
     * The open-config combo's first key is the vanilla advancements key (L by
     * default), which vanilla acts on immediately at key press -- the advancements
     * screen would open before the second key can complete the combo. So while
     * in-game, the press is swallowed here and the combo gets a chance to finish;
     * if the key is released without the config screen having opened, the
     * advancements screen is opened then, preserving the vanilla shortcut.
     */
    @Override
    public boolean onKeyInput(net.minecraft.client.input.KeyEvent event, boolean state) {
        int keyCode = event.key();
        Minecraft mc = Minecraft.getInstance();
        List<Integer> comboKeys = Hotkeys.OPEN_CONFIG_GUI.getKeybind().getKeys();
        if (comboKeys.size() < 2 || mc.options == null) {
            this.comboFirstKeyHeld = false;
            return false;
        }

        int advancementsKey = KeybindMulti.getKeyCode(mc.options.keyAdvancements);
        int firstComboKey = comboKeys.get(0);
        if (firstComboKey != advancementsKey || keyCode != firstComboKey) {
            return false;
        }

        boolean inGame = mc.level != null && mc.player != null && mc.screen == null;
        if (state) {
            if (inGame) {
                this.comboFirstKeyHeld = true;
                return true;
            }
            return false;
        }

        if (this.comboFirstKeyHeld) {
            this.comboFirstKeyHeld = false;
            // Any screen open at release means the combo (or something else)
            // already took over; only then skip the deferred vanilla action.
            if (inGame) {
                mc.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements()));
            }
        }
        return false;
    }
}
