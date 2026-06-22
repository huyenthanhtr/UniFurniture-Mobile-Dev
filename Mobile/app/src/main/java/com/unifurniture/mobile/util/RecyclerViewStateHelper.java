package com.unifurniture.mobile.util;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Saves and restores {@link RecyclerView} scroll position across view re-creation
 * (tab switches, back navigation) via fragment {@code onSaveInstanceState}.
 */
public final class RecyclerViewStateHelper {

    private final String key;
    @Nullable
    private Parcelable pendingState;
    @Nullable
    private RecyclerView recyclerView;

    public RecyclerViewStateHelper(@NonNull String key) {
        this.key = "rv_state_" + key;
    }

    public void bind(@NonNull RecyclerView recyclerView, @Nullable Bundle savedInstanceState) {
        this.recyclerView = recyclerView;
        if (savedInstanceState != null) {
            pendingState = savedInstanceState.getParcelable(key);
        }
    }

    public void save(@NonNull Bundle outState) {
        if (recyclerView == null) return;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            outState.putParcelable(key, layoutManager.onSaveInstanceState());
        }
    }

    /** Call from onStop() to capture scroll before view is destroyed (NavComponent back-stack). */
    public void savePending() {
        if (recyclerView == null) return;
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (lm != null) {
            pendingState = lm.onSaveInstanceState();
        }
    }

    /** Pass to {@code ListAdapter.submitList(list, callback)} to restore scroll after diff. */
    @NonNull
    public Runnable afterSubmitCallback() {
        return this::restoreIfPending;
    }

    public void restoreIfPending() {
        if (pendingState == null || recyclerView == null) return;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.onRestoreInstanceState(pendingState);
            pendingState = null;
        }
    }
}
