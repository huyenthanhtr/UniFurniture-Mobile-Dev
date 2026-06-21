package com.unifurniture.mobile.util;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * Saves and restores vertical scroll offset for {@link ScrollView} / {@link NestedScrollView}.
 */
public final class ScrollStateHelper {

    private final String key;
    private int pendingScrollY = -1;

    public ScrollStateHelper(@NonNull String key) {
        this.key = "scroll_y_" + key;
    }

    public void read(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pendingScrollY = savedInstanceState.getInt(key, -1);
        }
    }

    /** Call from onStop() to persist scroll across view-only recreation (NavComponent back stack). */
    public void saveScroll(@NonNull View scrollView) {
        int y = getScrollY(scrollView);
        if (y >= 0) pendingScrollY = y;
    }

    public void save(@NonNull Bundle outState, @NonNull View scrollView) {
        int scrollY = getScrollY(scrollView);
        if (scrollY > 0) {
            outState.putInt(key, scrollY);
        }
    }

    public void restore(@NonNull View scrollView) {
        if (pendingScrollY < 0) return;
        final int y = pendingScrollY;
        pendingScrollY = -1;
        scrollView.post(() -> scrollView.scrollTo(0, y));
    }

    private static int getScrollY(@NonNull View scrollView) {
        if (scrollView instanceof NestedScrollView nestedScrollView) {
            return nestedScrollView.getScrollY();
        }
        if (scrollView instanceof ScrollView scrollViewWidget) {
            return scrollViewWidget.getScrollY();
        }
        return 0;
    }
}
