package io.github.huanmeng06.lmlp.gui;

import java.util.List;

import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_465;

public interface RecipeTransferBridge {
    RecipeTransferBridge DISABLED = new RecipeTransferBridge() {
    };

    default TransferState evaluate(RecipeSummary summary, class_465<?> containerScreen) {
        return TransferState.UNSUPPORTED;
    }

    default Bounds buttonBounds(RecipeSummary summary, int panelX, int panelY, int panelWidth, int panelHeight) {
        return new Bounds(panelX + panelWidth + 2, panelY + panelHeight - 16, 10, 10);
    }

    default boolean transfer(RecipeSummary summary, class_465<?> containerScreen, boolean maxTransfer) {
        return false;
    }

    default void renderError(RecipeSummary summary, TransferState state, class_332 context, int mouseX, int mouseY) {
    }

    record Bounds(int x, int y, int width, int height) {
    }

    record TransferState(boolean supported, boolean enabled, boolean tinted, int tint, String label, List<class_2561> tooltip, Object nativeState) {
        public static final TransferState UNSUPPORTED = new TransferState(false, false, false, 0, "!", List.of(), null);

        public TransferState {
            label = label == null ? "" : label;
            tooltip = List.copyOf(tooltip);
        }
    }
}
