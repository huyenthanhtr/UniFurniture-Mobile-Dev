package com.unifurniture.mobile.util;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

/**
 * Scopes a ViewModel to a Navigation back stack entry so it survives bottom-tab switches
 * when {@code saveState}/{@code restoreState} is enabled in {@code MainActivity}.
 */
public final class NavViewModelProvider {

    private NavViewModelProvider() {
    }

    @NonNull
    public static <T extends ViewModel> T get(
            @NonNull Fragment fragment,
            @IdRes int destinationId,
            @NonNull Class<T> modelClass) {
        NavController navController = NavHostFragment.findNavController(fragment);
        NavBackStackEntry entry = findBackStackEntry(navController, destinationId);
        return new ViewModelProvider(entry).get(modelClass);
    }

    @Nullable
    private static NavBackStackEntry findBackStackEntry(
            @NonNull NavController navController,
            @IdRes int destinationId) {
        try {
            return navController.getBackStackEntry(destinationId);
        } catch (IllegalArgumentException ignored) {
            return navController.getCurrentBackStackEntry();
        }
    }
}
