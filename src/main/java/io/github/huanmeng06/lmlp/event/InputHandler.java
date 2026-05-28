package io.github.huanmeng06.lmlp.event;

import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import io.github.huanmeng06.lmlp.LitematicaMaterialListPlus;
import io.github.huanmeng06.lmlp.config.Hotkeys;

public class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();

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
}
