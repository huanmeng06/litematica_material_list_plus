package io.github.huanmeng06.lmlp.event;

import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Hotkeys;
import net.minecraft.class_310;
import net.minecraft.class_457;

import java.util.List;

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
    public boolean onKeyInput(net.minecraft.class_11908 event, boolean state) {
        int keyCode = event.comp_4795();
        class_310 mc = class_310.method_1551();
        List<Integer> comboKeys = Hotkeys.OPEN_CONFIG_GUI.getKeybind().getKeys();
        if (comboKeys.size() < 2 || mc.field_1690 == null) {
            this.comboFirstKeyHeld = false;
            return false;
        }

        int advancementsKey = KeybindMulti.getKeyCode(mc.field_1690.field_1844);
        int firstComboKey = comboKeys.get(0);
        if (firstComboKey != advancementsKey || keyCode != firstComboKey) {
            return false;
        }

        boolean inGame = mc.field_1687 != null && mc.field_1724 != null && mc.field_1755 == null;
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
                mc.method_1507(new class_457(mc.field_1724.field_3944.method_2869()));
            }
        }
        return false;
    }
}
