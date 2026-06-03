package io.github.huanmeng06.lmlp.access;

public interface WidgetListBoundsAccess {
    int lmlp$getVisibleTop();

    int lmlp$getVisibleBottom();

    void lmlp$scrollEntryIntoView(Object entry, int bottomPadding);
}
