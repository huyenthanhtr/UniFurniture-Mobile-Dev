package com.unifurniture.mobile.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private boolean syncingBottomNav = false;

    private static final int[] TOP_LEVEL_DESTINATIONS = {
            R.id.homeFragment,
            R.id.productListFragment,
            R.id.cartFragment,
            R.id.accountFragment
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.unifurniture.mobile.util.LanguageHelper.updateBaseContextLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (syncingBottomNav) {
                return true;
            }
            int itemId = item.getItemId();
            NavDestination current = navController.getCurrentDestination();

            if (current != null && getParentTabId(current.getId()) == itemId) {
                if (current.getId() != itemId) {
                    navController.popBackStack(itemId, false);
                }
                return true;
            }

            return navigateToTopLevelDestination(itemId);
        });

        syncBottomNavigationState(navController.getCurrentDestination());

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            syncBottomNavigationState(destination);
        });
    }

    private void syncBottomNavigationState(NavDestination destination) {
        if (destination == null) return;
        int tabId = getParentTabId(destination.getId());
        binding.bottomNavigation.setVisibility(shouldShowBottomNav(destination.getId()) ? View.VISIBLE : View.GONE);

        Menu menu = binding.bottomNavigation.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            boolean checked = menuItem.getItemId() == tabId;
            menuItem.setChecked(checked);
        }
        if (tabId != -1 && binding.bottomNavigation.getSelectedItemId() != tabId) {
            syncingBottomNav = true;
            binding.bottomNavigation.setSelectedItemId(tabId);
            syncingBottomNav = false;
        }
    }

    private boolean navigateToTopLevelDestination(@IdRes int destinationId) {
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();

        try {
            navController.navigate(destinationId, null, navOptions);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean isOnTabDestination(int currentId, @IdRes int tabId) {
        return getParentTabId(currentId) == tabId;
    }

    private boolean shouldShowBottomNav(@IdRes int destinationId) {
        return destinationId == R.id.homeFragment
                || destinationId == R.id.productListFragment
                || destinationId == R.id.cartFragment
                || destinationId == R.id.accountFragment;
    }

    @IdRes
    private static int getParentTabId(int destinationId) {
        if (destinationId == R.id.homeFragment ||
            destinationId == R.id.categoryFragment ||
            destinationId == R.id.promotionsFragment) {
            return R.id.homeFragment;
        } else if (destinationId == R.id.productListFragment ||
                   destinationId == R.id.wishlistFragment ||
                   destinationId == R.id.productDetailFragment) {
            return R.id.productListFragment;
        } else if (destinationId == R.id.cartFragment ||
                   destinationId == R.id.checkoutFragment ||
                   destinationId == R.id.paymentQrFragment ||
                   destinationId == R.id.orderSuccessFragment ||
                   destinationId == R.id.voucherListFragment) {
            return R.id.cartFragment;
        } else if (destinationId == R.id.accountFragment ||
                   destinationId == R.id.profileFragment ||
                   destinationId == R.id.changePasswordFragment ||
                   destinationId == R.id.addressBookFragment ||
                   destinationId == R.id.myReviewsFragment ||
                   destinationId == R.id.orderTrackingFragment ||
                   destinationId == R.id.orderListFragment ||
                   destinationId == R.id.orderDetailFragment ||
                   destinationId == R.id.notificationFragment ||
                   destinationId == R.id.contentFragment) {
            return R.id.accountFragment;
        } else {
            for (int tabId : TOP_LEVEL_DESTINATIONS) {
                if (destinationId == tabId) {
                    return tabId;
                }
            }
            return -1;
        }
    }
}
