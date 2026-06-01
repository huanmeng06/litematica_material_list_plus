package io.github.huanmeng06.lmlp.recipe.rei;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import io.github.huanmeng06.lmlp.gui.RecipeNativeDisplayBridge;
import io.github.huanmeng06.lmlp.recipe.RecipeSummary;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategoryView;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.class_1836;
import net.minecraft.class_327;
import net.minecraft.class_332;

public final class ReiNativeDisplayBridge implements RecipeNativeDisplayBridge {
    private static final int WORKSTATION_SLOT_SIZE = 18;
    private static final int WORKSTATION_SLOT_MARGIN = 4;

    private final Map<Display, Layout> layouts = new IdentityHashMap<>();

    @Override
    public boolean canRender(RecipeSummary summary) {
        Display display = displayFor(summary);
        if (display == null) {
            return false;
        }

        try {
            return categoryFor(display) != null;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public int getDisplayWidth(RecipeSummary summary, int fallbackWidth) {
        Display display = displayFor(summary);
        if (display == null) {
            return fallbackWidth;
        }

        try {
            CategoryRegistry.CategoryConfiguration<Display> category = categoryFor(display);
            return category == null ? fallbackWidth : Math.max(1, category.getCategory().getDisplayWidth(display));
        } catch (Throwable throwable) {
            return fallbackWidth;
        }
    }

    @Override
    public int getDisplayHeight(RecipeSummary summary, int fallbackHeight) {
        Display display = displayFor(summary);
        if (display == null) {
            return fallbackHeight;
        }

        try {
            CategoryRegistry.CategoryConfiguration<Display> category = categoryFor(display);
            return category == null ? fallbackHeight : Math.max(1, category.getCategory().getDisplayHeight());
        } catch (Throwable throwable) {
            return fallbackHeight;
        }
    }

    @Override
    public void render(RecipeSummary summary, class_332 context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
        Display display = requireDisplay(summary);
        Layout layout = this.layoutFor(display, x, y, width, height);
        Widget.pushMouse(new Point(mouseX, mouseY));
        try {
            for (Widget widget : layout.widgets()) {
                widget.method_25394(context, mouseX, mouseY, delta);
            }
        } finally {
            Widget.popMouse();
        }
    }

    @Override
    public boolean renderTooltip(RecipeSummary summary, class_332 context, class_327 textRenderer, int mouseX, int mouseY) {
        Display display = displayFor(summary);
        Layout layout = display == null ? null : this.layouts.get(display);
        if (layout == null) {
            return false;
        }

        Widget.pushMouse(new Point(mouseX, mouseY));
        try {
            for (int i = layout.widgets().size() - 1; i >= 0; i--) {
                Widget widget = layout.widgets().get(i);
                if (widget instanceof Slot slot && slot.isTooltipsEnabled() && slot.containsMouse(mouseX, mouseY)) {
                    Tooltip tooltip = slot.getCurrentTooltip(TooltipContext.of(new Point(mouseX, mouseY), class_1836.field_41070));
                    return tooltip != null && ReiTooltipBridge.renderTooltip(context, textRenderer, tooltip, mouseX, mouseY);
                }
            }
        } finally {
            Widget.popMouse();
        }

        return false;
    }

    @Override
    public boolean mouseClicked(RecipeSummary summary, double mouseX, double mouseY, int button) {
        return this.dispatch(summary, widget -> widget.method_25402(mouseX, mouseY, button), mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(RecipeSummary summary, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.dispatch(summary, widget -> widget.method_25403(mouseX, mouseY, button, deltaX, deltaY), mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(RecipeSummary summary, double mouseX, double mouseY, int button) {
        return this.dispatch(summary, widget -> widget.method_25406(mouseX, mouseY, button), mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(RecipeSummary summary, double mouseX, double mouseY, double amount) {
        return this.dispatch(summary, widget -> widget.method_25401(mouseX, mouseY, 0.0D, amount), mouseX, mouseY);
    }

    private boolean dispatch(RecipeSummary summary, WidgetAction action, double mouseX, double mouseY) {
        Display display = displayFor(summary);
        Layout layout = display == null ? null : this.layouts.get(display);
        if (layout == null) {
            return false;
        }

        Widget.pushMouse(new Point(mouseX, mouseY));
        try {
            for (int i = layout.widgets().size() - 1; i >= 0; i--) {
                if (action.apply(layout.widgets().get(i))) {
                    return true;
                }
            }
        } finally {
            Widget.popMouse();
        }

        return false;
    }

    private Layout layoutFor(Display display, int x, int y, int width, int height) {
        Layout existing = this.layouts.get(display);
        if (existing != null && existing.matches(x, y, width, height)) {
            return existing;
        }

        Rectangle bounds = new Rectangle(x, y, width, height);
        EntryStack<?> workstationEntry = workstationEntry(display);

        CategoryRegistry.CategoryConfiguration<Display> category = categoryFor(display);
        if (category == null) {
            throw new IllegalStateException("No REI category for display " + display.getCategoryIdentifier());
        }

        DisplayCategoryView<Display> view = category.getView(display);
        List<Widget> widgets = new ArrayList<>(view.setupDisplay(display, bounds));
        if (workstationEntry != null) {
            widgets.add(this.createWorkstationSlot(bounds, workstationEntry));
        }

        Layout layout = new Layout(bounds, List.copyOf(widgets));
        this.layouts.put(display, layout);
        return layout;
    }

    private Slot createWorkstationSlot(Rectangle bounds, EntryStack<?> workstationEntry) {
        Rectangle slotBounds = workstationSlotBounds(bounds);
        return Widgets.createSlot(slotBounds)
                .entry(workstationEntry.copy())
                .interactable(true)
                .interactableFavorites(true)
                .highlightEnabled(true)
                .tooltipsEnabled(true);
    }

    private static Display requireDisplay(RecipeSummary summary) {
        Display display = displayFor(summary);
        if (display == null) {
            throw new IllegalArgumentException("Recipe summary has no REI display");
        }

        return display;
    }

    private static Display displayFor(RecipeSummary summary) {
        Object nativeDisplay = summary.nativeDisplay();
        return nativeDisplay instanceof Display display ? display : null;
    }

    private static EntryStack<?> workstationEntry(Display display) {
        if (isCraftingCategory(display)) {
            return null;
        }

        CategoryRegistry.CategoryConfiguration<Display> category = categoryFor(display);
        if (category == null) {
            return null;
        }

        for (EntryIngredient ingredient : category.getWorkstations()) {
            for (EntryStack<?> stack : ingredient) {
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        }

        return null;
    }

    private static Rectangle workstationSlotBounds(Rectangle bounds) {
        return new Rectangle(
                bounds.x + WORKSTATION_SLOT_MARGIN,
                bounds.y + bounds.height - WORKSTATION_SLOT_SIZE - WORKSTATION_SLOT_MARGIN,
                WORKSTATION_SLOT_SIZE,
                WORKSTATION_SLOT_SIZE);
    }

    private static boolean isCraftingCategory(Display display) {
        String category = display.getCategoryIdentifier().getIdentifier().toString();
        return category.equals("minecraft:crafting")
                || category.equals("minecraft:plugins/crafting")
                || category.endsWith(":crafting")
                || category.endsWith("/crafting");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CategoryRegistry.CategoryConfiguration<Display> categoryFor(Display display) {
        return (CategoryRegistry.CategoryConfiguration) CategoryRegistry.getInstance().get((CategoryIdentifier) display.getCategoryIdentifier());
    }

    @FunctionalInterface
    private interface WidgetAction {
        boolean apply(Widget widget);
    }

    private record Layout(Rectangle bounds, List<Widget> widgets) {
        private boolean matches(int x, int y, int width, int height) {
            return this.bounds.x == x && this.bounds.y == y && this.bounds.width == width && this.bounds.height == height;
        }
    }
}
