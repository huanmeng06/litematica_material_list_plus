package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.hotkeys.IKeybind;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class KeybindResetRefreshRegistry {
    private static final List<Binding> BINDINGS = new ArrayList<>();

    private KeybindResetRefreshRegistry() {
    }

    public static void register(IKeybind keybind, ButtonGeneric resetButton) {
        if (keybind == null || resetButton == null) {
            return;
        }

        cleanup();
        BINDINGS.add(new Binding(new WeakReference<>(keybind), new WeakReference<>(resetButton)));
        update(keybind, resetButton);
    }

    public static void refresh() {
        Iterator<Binding> iterator = BINDINGS.iterator();
        while (iterator.hasNext()) {
            Binding binding = iterator.next();
            IKeybind keybind = binding.keybind().get();
            ButtonGeneric resetButton = binding.resetButton().get();
            if (keybind == null || resetButton == null) {
                iterator.remove();
                continue;
            }
            update(keybind, resetButton);
        }
    }

    private static void cleanup() {
        BINDINGS.removeIf(binding -> binding.keybind().get() == null || binding.resetButton().get() == null);
    }

    private static void update(IKeybind keybind, ButtonGeneric resetButton) {
        resetButton.setEnabled(!Objects.equals(
                keybind.getStringValue(),
                keybind.getDefaultStringValue()));
    }

    private record Binding(WeakReference<IKeybind> keybind, WeakReference<ButtonGeneric> resetButton) {
    }
}
