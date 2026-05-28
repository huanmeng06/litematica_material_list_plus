package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.class_332;
import net.minecraft.class_465;

public final class MaterialListOverlay {
    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 8;
    private static final int MIN_WIDTH = 360;
    private static final int MAX_WIDTH = 620;
    private static final int MIN_HEIGHT = 220;
    private static final int MAX_HEIGHT = 420;

    private static MaterialListOverlay active;

    private final class_465<?> owner;
    private final GuiMaterialList gui;
    private WidgetListMaterialList listWidget;
    private int screenWidth;
    private int screenHeight;
    private int left;
    private int top;
    private int width;
    private int height;

    private MaterialListOverlay(class_465<?> owner, MaterialListBase materialList, int screenWidth, int screenHeight) {
        this.owner = owner;
        this.gui = new GuiMaterialList(materialList);
        this.rebuild(screenWidth, screenHeight);
    }

    public static boolean toggle(class_465<?> owner, int screenWidth, int screenHeight) {
        if (active != null && active.owner == owner) {
            active = null;
            return true;
        }

        MaterialListBase materialList = MaterialListOpener.getOrCreateMaterialList();
        if (materialList == null) {
            return true;
        }

        active = new MaterialListOverlay(owner, materialList, screenWidth, screenHeight);
        return true;
    }

    public static boolean isActiveFor(class_465<?> owner) {
        return active != null && active.owner == owner;
    }

    public static void close(class_465<?> owner) {
        if (isActiveFor(owner)) {
            active = null;
        }
    }

    public static boolean render(class_465<?> owner, class_332 context, int mouseX, int mouseY, float delta, int screenWidth, int screenHeight) {
        MaterialListOverlay overlay = activeFor(owner);
        if (overlay == null) {
            return false;
        }

        overlay.render(context, mouseX, mouseY, delta, screenWidth, screenHeight);
        return true;
    }

    public static boolean mouseClicked(class_465<?> owner, double mouseX, double mouseY, int button) {
        MaterialListOverlay overlay = activeFor(owner);
        return overlay != null && overlay.mouseClicked((int) mouseX, (int) mouseY, button);
    }

    public static boolean mouseReleased(class_465<?> owner, double mouseX, double mouseY, int button) {
        MaterialListOverlay overlay = activeFor(owner);
        return overlay != null && overlay.mouseReleased((int) mouseX, (int) mouseY, button);
    }

    public static boolean mouseScrolled(class_465<?> owner, double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        MaterialListOverlay overlay = activeFor(owner);
        return overlay != null && overlay.mouseScrolled((int) mouseX, (int) mouseY, horizontalAmount, verticalAmount);
    }

    public static boolean keyPressed(class_465<?> owner, int keyCode, int scanCode, int modifiers) {
        MaterialListOverlay overlay = activeFor(owner);
        return overlay != null && overlay.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean charTyped(class_465<?> owner, char chr, int modifiers) {
        MaterialListOverlay overlay = activeFor(owner);
        return overlay != null && overlay.charTyped(chr, modifiers);
    }

    private static MaterialListOverlay activeFor(class_465<?> owner) {
        return isActiveFor(owner) ? active : null;
    }

    private void render(class_332 context, int mouseX, int mouseY, float delta, int screenWidth, int screenHeight) {
        if (this.screenWidth != screenWidth || this.screenHeight != screenHeight) {
            this.rebuild(screenWidth, screenHeight);
        }

        RenderUtils.drawRect(0, 0, screenWidth, screenHeight, 0x66000000);
        RenderUtils.drawOutlinedBox(this.left, this.top, this.width, this.height, 0xEE101010, 0xFF777777);
        RenderUtils.drawRect(this.left + 1, this.top + 1, this.width - 2, HEADER_HEIGHT, 0xCC202020);
        context.method_51433(this.gui.textRenderer, this.gui.getMaterialList().getTitle(), this.left + 8, this.top + 7, 0xFFFFFFFF, false);
        context.method_51433(this.gui.textRenderer, GuiBase.TXT_GRAY + "Esc", this.left + this.width - 28, this.top + 7, 0xFFFFFFFF, false);
        this.listWidget.drawContents(context, mouseX, mouseY, delta);
    }

    private boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!this.isInside(mouseX, mouseY)) {
            active = null;
            return true;
        }

        return this.listWidget.onMouseClicked(mouseX, mouseY, button) || true;
    }

    private boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (!this.isInside(mouseX, mouseY)) {
            return true;
        }

        return this.listWidget.onMouseReleased(mouseX, mouseY, button) || true;
    }

    private boolean mouseScrolled(int mouseX, int mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.isInside(mouseX, mouseY)) {
            return true;
        }

        return this.listWidget.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) || true;
    }

    private boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            active = null;
            return true;
        }

        return this.listWidget.onKeyTyped(keyCode, scanCode, modifiers);
    }

    private boolean charTyped(char chr, int modifiers) {
        return this.listWidget.onCharTyped(chr, modifiers);
    }

    private void rebuild(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.width = clamp(screenWidth - MARGIN * 2, MIN_WIDTH, MAX_WIDTH);
        this.height = clamp(screenHeight - MARGIN * 2, MIN_HEIGHT, MAX_HEIGHT);
        this.left = Math.max(MARGIN, (screenWidth - this.width) / 2);
        this.top = Math.max(MARGIN, (screenHeight - this.height) / 2);

        int listX = this.left + 8;
        int listY = this.top + HEADER_HEIGHT + 6;
        int listWidth = Math.max(1, this.width - 16);
        int listHeight = Math.max(1, this.height - HEADER_HEIGHT - FOOTER_HEIGHT - 8);
        this.listWidget = new WidgetListMaterialList(listX, listY, listWidth, listHeight, this.gui);
        this.listWidget.initGui();
    }

    private boolean isInside(int mouseX, int mouseY) {
        return mouseX >= this.left && mouseX < this.left + this.width && mouseY >= this.top && mouseY < this.top + this.height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
