package io.github.huanmeng06.lmlp.recipe.jei;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.huanmeng06.lmlp.gui.RecipeTransferBridge;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.class_1703;
import net.minecraft.class_8786;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_465;
import net.minecraft.class_768;

public final class JeiRecipeTransferBridge implements RecipeTransferBridge {
    // REI TransferHandler.Result's original failed-transfer tint.
    private static final int REI_FAILED_TRANSFER_TINT = 0x67FF0000;

    @Override
    public TransferState evaluate(RecipeSummary summary, class_465<?> containerScreen) {
        try {
            TransferLookup lookup = transferLookup(summary, containerScreen);
            if (lookup == null) {
                return TransferState.UNSUPPORTED;
            }

            if (lookup.handler().isEmpty()) {
                return new TransferState(
                        true,
                        false,
                        false,
                        0,
                        "!",
                        tooltip(summary, "error.rei.not.supported.move.items"),
                        null);
            }

            TransferContext context = new TransferContext(lookup.handler().get(), lookup.menu(), lookup.nativeRecipe());
            IRecipeTransferError error = context.check(false, false);
            if (error == null) {
                return new TransferState(true, true, false, 0, "+", transferTooltip(), null);
            }

            if (error.getType() == IRecipeTransferError.Type.INTERNAL) {
                return new TransferState(
                        true,
                        false,
                        false,
                        0,
                        "!",
                        tooltip(summary, "error.rei.not.supported.move.items"),
                        error);
            }

            boolean enabled = error.getType().allowsTransfer;
            return new TransferState(
                    true,
                    enabled,
                    !enabled,
                    enabled ? 0 : REI_FAILED_TRANSFER_TINT,
                    "+",
                    enabled ? transferTooltip() : errorTooltip(error),
                    error);
        } catch (Throwable throwable) {
            return TransferState.UNSUPPORTED;
        }
    }

    @Override
    public Bounds buttonBounds(RecipeSummary summary, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Keep the transfer control outside the recipe panel with enough clearance
        // that the 10 x 10 button does not cover the visible background edge.
        return new Bounds(panelX + panelWidth + 5, panelY + panelHeight - 8, 10, 10);
    }

    @Override
    public boolean transfer(RecipeSummary summary, class_465<?> containerScreen, boolean maxTransfer) {
        try {
            if (!maxTransfer && summary.craftsMissing() <= 0) {
                return false;
            }
            TransferLookup lookup = transferLookup(summary, containerScreen);
            if (lookup == null || lookup.handler().isEmpty()) {
                return false;
            }

            TransferContext context = new TransferContext(lookup.handler().get(), lookup.menu(), lookup.nativeRecipe());
            IRecipeTransferError error = context.transfer(summary.craftsMissing(), maxTransfer);
            return error == null || error.getType().allowsTransfer;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public void renderError(RecipeSummary summary, TransferState state, class_332 context, int mouseX, int mouseY) {
        if (!(state.nativeState() instanceof IRecipeTransferError error)) {
            return;
        }

        JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe = nativeRecipe(summary);
        if (nativeRecipe == null) {
            return;
        }

        class_768 recipeBounds = nativeRecipe.layout().getRect();
        Collection<?> slots = missingSlots(error);
        context.method_51448().method_22903();
        context.method_51448().method_46416(
                recipeBounds.method_3321(),
                recipeBounds.method_3322(),
                0.0F);
        try {
            for (Object rawSlot : slots) {
                if (rawSlot instanceof IRecipeSlotView slot) {
                    slot.drawHighlight(context, 0x66FF0000);
                }
            }
        } finally {
            context.method_51448().method_22909();
        }
    }

    @Override
    public boolean renderErrorIncludesTooltip(TransferState state) {
        return false;
    }

    private static Collection<?> missingSlots(IRecipeTransferError error) {
        Class<?> type = error.getClass();
        while (type != null) {
            try {
                Field slotsField = type.getDeclaredField("slots");
                slotsField.setAccessible(true);
                if (slotsField.get(error) instanceof Collection<?> slots) {
                    return slots;
                }
            } catch (ReflectiveOperationException ignored) {
                // Keep looking through JEI's error class hierarchy.
            }
            type = type.getSuperclass();
        }
        return List.of();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static TransferLookup transferLookup(RecipeSummary summary, class_465<?> containerScreen) {
        IJeiRuntime runtime = JeiRuntimeBridge.runtime().orElse(null);
        JeiRecipeResolver.JeiNativeRecipe nativeRecipe = nativeRecipe(summary);
        if (runtime == null || nativeRecipe == null || containerScreen == null || class_310.method_1551().field_1724 == null) {
            return null;
        }

        class_1703 menu = containerScreen.method_17577();
        Optional<IRecipeTransferHandler<class_1703, Object>> handler = (Optional) runtime.getRecipeTransferManager()
                .getRecipeTransferHandler(menu, nativeRecipe.category());
        return new TransferLookup(handler, menu, nativeRecipe);
    }

    private static List<class_2561> tooltip(RecipeSummary summary, String primaryKey) {
        List<class_2561> lines = new java.util.ArrayList<>();
        lines.add(class_2561.method_43471(primaryKey));
        if (class_310.method_1551().field_1690 != null && class_310.method_1551().field_1690.field_1827) {
            String recipeIdLabel = class_2561.method_43471("lmlp.label.recipe.recipe_id").getString();
            lines.add(class_2561.method_43470(recipeIdLabel + ": " + summary.recipeId())
                    .method_27692(class_124.field_1080));
        }
        return List.copyOf(lines);
    }

    private static List<class_2561> transferTooltip() {
        return List.of(
                class_2561.method_43471("lmlp.tooltip.recipe.transfer_missing"),
                class_2561.method_43471("lmlp.tooltip.recipe.transfer_all"));
    }

    private static List<class_2561> errorTooltip(IRecipeTransferError error) {
        Class<?> type = error.getClass();
        while (type != null) {
            try {
                Field messageField = type.getDeclaredField("message");
                messageField.setAccessible(true);
                if (messageField.get(error) instanceof List<?> rawLines) {
                    List<class_2561> lines = new ArrayList<>();
                    for (Object rawLine : rawLines) {
                        if (rawLine instanceof class_2561 line) {
                            lines.add(line);
                        }
                    }
                    if (!lines.isEmpty()) {
                        return List.copyOf(lines);
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Older and newer JEI versions expose this message differently.
            }
            type = type.getSuperclass();
        }

        return List.of(
                class_2561.method_43471("jei.tooltip.transfer"),
                class_2561.method_43471("jei.tooltip.error.recipe.transfer.missing")
                        .method_27692(class_124.field_1061));
    }

    private static JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe(RecipeSummary summary) {
        return summary.nativeDisplay() instanceof JeiRecipeResolver.JeiNativeRecipe<?> nativeRecipe ? nativeRecipe : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private record TransferLookup(Optional<IRecipeTransferHandler<class_1703, Object>> handler, class_1703 menu, JeiRecipeResolver.JeiNativeRecipe nativeRecipe) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private record TransferContext(IRecipeTransferHandler handler, class_1703 menu, JeiRecipeResolver.JeiNativeRecipe nativeRecipe) {
        private IRecipeTransferError check(boolean maxTransfer, boolean doTransfer) {
            return this.handler.transferRecipe(
                    this.menu,
                    this.nativeRecipe.recipe(),
                    this.nativeRecipe.layout().getRecipeSlotsView(),
                    class_310.method_1551().field_1724,
                    maxTransfer,
                    doTransfer);
        }

        private IRecipeTransferError transfer(int craftsMissing, boolean maxTransfer) {
            if (maxTransfer || craftsMissing <= 1) {
                return this.check(maxTransfer, true);
            }
            IRecipeTransferError error = this.check(false, true);
            if (error == null || error.getType().allowsTransfer) {
                Object recipe = this.nativeRecipe.recipe();
                if (recipe instanceof class_8786 vanillaRecipe
                        && class_310.method_1551().field_1761 != null) {
                    for (int craft = 1; craft < craftsMissing; craft++) {
                        class_310.method_1551().field_1761.method_2912(
                                this.menu.field_7763,
                                vanillaRecipe,
                                false);
                    }
                }
            }
            return error;
        }
    }
}
