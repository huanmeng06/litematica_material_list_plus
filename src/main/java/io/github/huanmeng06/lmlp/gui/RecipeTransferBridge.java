package io.github.huanmeng06.lmlp.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;

public interface RecipeTransferBridge {
    RecipeTransferBridge DISABLED = new RecipeTransferBridge() {
    };

    default TransferState evaluate(RecipeSummary summary, AbstractContainerScreen<?> containerScreen) {
        return TransferState.UNSUPPORTED;
    }

    default Bounds buttonBounds(RecipeSummary summary, int panelX, int panelY, int panelWidth, int panelHeight) {
        return new Bounds(panelX + panelWidth + 2, panelY + panelHeight - 16, 10, 10);
    }

    default boolean transfer(RecipeSummary summary, AbstractContainerScreen<?> containerScreen, boolean maxTransfer) {
        return false;
    }

    default void renderError(RecipeSummary summary, TransferState state, GuiGraphicsExtractor context, int mouseX, int mouseY) {
    }

    record Bounds(int x, int y, int width, int height) {
    }

    record TransferState(boolean supported, boolean enabled, boolean tinted, int tint, String label, List<Component> tooltip, Object nativeState) {
        public static final TransferState UNSUPPORTED = new TransferState(false, false, false, 0, "!", List.of(), null);

        public TransferState {
            label = label == null ? "" : label;
            tooltip = List.copyOf(tooltip);
        }
    }
}
