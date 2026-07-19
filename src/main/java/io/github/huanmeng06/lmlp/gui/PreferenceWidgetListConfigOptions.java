package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.render.GuiContext;
import io.github.huanmeng06.lmlp.config.Configs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Native MaLiLib config list with compact, animated preference groups.
 *
 * <p>Each group owns one boolean row followed immediately by its dependent rows. A dependent
 * row is removed entirely while its group is closed, so later groups naturally move up without
 * leaving holes. Adding another preference category only requires appending another descriptor
 * to {@link #GROUPS}.</p>
 */
final class PreferenceWidgetListConfigOptions extends WidgetListConfigOptions {
    private static final List<PreferenceGroup> GROUPS = List.of(
            new PreferenceGroup(
                    "preferred_wood",
                    Configs.ConfigForms.PREFERRED_WOOD_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_WOOD_FAMILY)
            ),
            new PreferenceGroup(
                    "preferred_stone",
                    Configs.ConfigForms.PREFERRED_STONE_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_STONE_FAMILY)
            ),
            new PreferenceGroup(
                    "preferred_glass",
                    Configs.ConfigForms.PREFERRED_GLASS_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_GLASS_MATERIAL)
            ),
            new PreferenceGroup(
                    "preferred_carpet",
                    Configs.ConfigForms.PREFERRED_CARPET_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_CARPET_MATERIAL)
            ),
            new PreferenceGroup(
                    "preferred_terracotta",
                    Configs.ConfigForms.PREFERRED_TERRACOTTA_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_TERRACOTTA_MATERIAL)
            ),
            new PreferenceGroup(
                    "preferred_glazed_terracotta",
                    Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_ENABLED,
                    List.of(Configs.ConfigForms.PREFERRED_GLAZED_TERRACOTTA_MATERIAL)
            )
    );

    private final ExpandAnimationTracker animations = new ExpandAnimationTracker();

    PreferenceWidgetListConfigOptions(
            int x,
            int y,
            int width,
            int height,
            int configWidth,
            float zLevel,
            boolean keybindSearch,
            GuiConfigsBase parent) {
        super(x, y, width, height, configWidth, zLevel, keybindSearch, parent);
    }

    void setGroupExpanded(IConfigBoolean toggle, boolean expanded) {
        PreferenceGroup group = findGroupByToggle(toggle);
        if (group == null) {
            return;
        }

        float currentProgress = this.animations.progress(group.animationKey(), !expanded);
        this.animations.start(group.animationKey(), currentProgress, expanded ? 1.0F : 0.0F);

        // Rebuilding animated rows must not erase MaLiLib's modified-state bookkeeping.
        this.markConfigsModified();
        this.refreshEntries();
    }

    boolean isMouseOverVisibleEntries(int mouseX, int mouseY) {
        int top = this.browserEntriesStartY + this.browserEntriesOffsetY;
        int bottom = this.browserEntriesStartY + this.browserHeight - 8;
        return mouseX >= this.browserEntriesStartX
                && mouseX < this.browserEntriesStartX + this.browserEntryWidth
                && mouseY >= top
                && mouseY < bottom;
    }

    @Override
    protected Collection<GuiConfigsBase.ConfigOptionWrapper> getAllEntries() {
        Collection<GuiConfigsBase.ConfigOptionWrapper> allEntries = super.getAllEntries();
        List<GuiConfigsBase.ConfigOptionWrapper> visibleEntries = new ArrayList<>(allEntries.size());

        for (GuiConfigsBase.ConfigOptionWrapper wrapper : allEntries) {
            PreferenceGroup group = findGroupByToggle(wrapper.getConfig());
            if (group != null) {
                visibleEntries.add(wrapper);
                if (this.groupProgress(group) > 0.0F) {
                    for (IConfigBase child : group.children()) {
                        GuiConfigsBase.ConfigOptionWrapper childWrapper = findWrapper(allEntries, child);
                        if (childWrapper != null) {
                            visibleEntries.add(childWrapper);
                        }
                    }
                }
            } else if (findGroupByChild(wrapper.getConfig()) == null) {
                visibleEntries.add(wrapper);
            }
        }

        return visibleEntries;
    }

    @Override
    protected int getBrowserEntryHeightFor(GuiConfigsBase.ConfigOptionWrapper wrapper) {
        PreferenceGroup owner = findGroupByChild(wrapper.getConfig());
        if (owner == null) {
            return super.getBrowserEntryHeightFor(wrapper);
        }

        int height = this.visibleHeight(owner);
        if (this.parent instanceof GuiPreferredMaterialForm form) {
            height += form.inlineDetailHeight(wrapper.getConfig());
        }
        return height;
    }

    @Override
    protected WidgetConfigOption createListEntryWidget(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            GuiConfigsBase.ConfigOptionWrapper wrapper) {
        PreferenceGroup owner = findGroupByChild(wrapper.getConfig());
        PreferenceGroup toggleGroup = findGroupByToggle(wrapper.getConfig());
        boolean materialFormEntry = this.parent instanceof GuiPreferredMaterialForm
                && (owner != null || toggleGroup != null);
        if (owner == null && !materialFormEntry) {
            return super.createListEntryWidget(x, y, listIndex, isOdd, wrapper);
        }

        int visibleHeight = owner == null ? this.browserEntryHeight : this.visibleHeight(owner);
        int detailHeight = owner != null && this.parent instanceof GuiPreferredMaterialForm form
                ? form.inlineDetailHeight(wrapper.getConfig())
                : 0;
        return new AnimatedPreferenceConfigOption(
                x,
                y,
                this.browserEntryWidth,
                this.browserEntryHeight,
                visibleHeight,
                detailHeight,
                this.maxLabelWidth,
                this.configWidth,
                wrapper,
                listIndex,
                this.parent,
                this
        );
    }

    @Override
    public void drawContents(GuiContext context, int mouseX, int mouseY, float partialTicks) {
        if (this.animations.isActive()) {
            this.refreshEntries();
        }

        super.drawContents(context, mouseX, mouseY, partialTicks);

        if (this.animations.isActive()) {
            this.animations.prune();
            if (!this.animations.isActive()) {
                // One final rebuild removes fully collapsed child rows from filtering and layout.
                this.refreshEntries();
            }
        }
    }

    private int visibleHeight(PreferenceGroup group) {
        return Math.max(1, Math.round(this.browserEntryHeight * this.groupProgress(group)));
    }

    private float groupProgress(PreferenceGroup group) {
        return this.animations.progress(group.animationKey(), group.toggle().getBooleanValue());
    }

    private static PreferenceGroup findGroupByToggle(IConfigBase toggle) {
        for (PreferenceGroup group : GROUPS) {
            if (group.toggle() == toggle) {
                return group;
            }
        }
        return null;
    }

    private static GuiConfigsBase.ConfigOptionWrapper findWrapper(
            Collection<GuiConfigsBase.ConfigOptionWrapper> wrappers,
            IConfigBase config) {
        for (GuiConfigsBase.ConfigOptionWrapper wrapper : wrappers) {
            if (wrapper.getConfig() == config) {
                return wrapper;
            }
        }
        return null;
    }

    private static PreferenceGroup findGroupByChild(IConfigBase config) {
        if (config == null) {
            return null;
        }

        for (PreferenceGroup group : GROUPS) {
            for (IConfigBase child : group.children()) {
                if (child == config) {
                    return group;
                }
            }
        }
        return null;
    }

    private record PreferenceGroup(
            String animationKey,
            IConfigBoolean toggle,
            List<? extends IConfigBase> children) {
    }
}
