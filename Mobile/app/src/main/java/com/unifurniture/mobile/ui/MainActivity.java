package com.unifurniture.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.ActivityMainBinding;
import com.unifurniture.mobile.ui.auth.AuthActivity;

public class MainActivity extends AppCompatActivity {

    private static final float CHAT_FAB_DRAG_THRESHOLD_PX = 12f;
    private static final float CHAT_FAB_EDGE_MARGIN_PX = 16f;

    private ActivityMainBinding binding;
    private NavController navController;
    private boolean syncingBottomNav = false;

    // POST_NOTIFICATIONS runtime permission (Android 13+). Result ignored — we just ask once.
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    // Auth runs as a result-returning activity on top of MainActivity, so this activity (and its
    // ViewModels / caches / FragmentManager) stays alive while the user logs in or backs out.
    private final ActivityResultLauncher<Intent> authLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                int code = result.getResultCode();
                if (code == AuthActivity.RESULT_GO_HOME || code == AuthActivity.RESULT_LOGGED_IN) {
                    goToHome();
                }
                if (code == AuthActivity.RESULT_LOGGED_IN) {
                    // Session changed but this activity wasn't recreated, so re-fetch the cart for
                    // the new session to keep the bottom-nav badge in sync, and register the FCM
                    // token against the now-logged-in customer.
                    refreshCart();
                    com.unifurniture.mobile.messaging.DeviceTokenManager.syncToken(this);
                }
            });

    private float chatFabDownRawX;
    private float chatFabDownRawY;
    private float chatFabDownX;
    private float chatFabDownY;
    private boolean chatFabDragged;

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

        setupCartBadge();
        setupConnectivityBanner();

        // Push notifications: ask for permission (13+), register the FCM token, and honor any
        // deep link the launching notification carried.
        requestNotificationPermissionIfNeeded();
        com.unifurniture.mobile.messaging.DeviceTokenManager.syncToken(this);
        handleNotificationIntent(getIntent());

        // Floating assistant button — available on every screen except the chat itself.
        binding.fabChat.setOnClickListener(v -> {
            if (chatFabDragged) {
                chatFabDragged = false;
                return;
            }
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.chatFragment) {
                return;
            }
            try {
                navController.navigate(R.id.chatFragment);
            } catch (IllegalArgumentException ignored) {}
        });
        setupDraggableChatFab();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Route a notification tap (carried as intent extras) to the relevant screen. */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String orderId = intent.getStringExtra(com.unifurniture.mobile.messaging.FcmService.EXTRA_ORDER_ID);
        String deepLink = intent.getStringExtra(com.unifurniture.mobile.messaging.FcmService.EXTRA_DEEP_LINK);
        if (orderId == null && deepLink == null) return;
        try {
            if (orderId != null && !orderId.isEmpty()) {
                Bundle args = new Bundle();
                args.putString("order_id", orderId);
                navController.navigate(R.id.orderDetailFragment, args);
            } else {
                navController.navigate(R.id.notificationFragment);
            }
        } catch (IllegalArgumentException ignored) {
        }
        // Consume so a config change / re-entry doesn't navigate again.
        intent.removeExtra(com.unifurniture.mobile.messaging.FcmService.EXTRA_ORDER_ID);
        intent.removeExtra(com.unifurniture.mobile.messaging.FcmService.EXTRA_DEEP_LINK);
    }

    /** Open the login / registration flow without leaving MainActivity. The outcome is handled by
     *  {@link #authLauncher}. Call this from fragments instead of starting AuthActivity directly. */
    public void launchAuth() {
        authLauncher.launch(new Intent(this, AuthActivity.class));
    }

    /** Reset navigation to the Home tab in place — no Activity recreation, ViewModels preserved. */
    public void goToHome() {
        if (navController != null) {
            navigateToTopLevelDestination(R.id.homeFragment);
        }
    }

    private android.net.ConnectivityManager.NetworkCallback networkCallback;

    /** Show a thin banner whenever the device is offline; hide it when connectivity returns. */
    private void setupConnectivityBanner() {
        binding.tvOfflineBanner.setVisibility(
                com.unifurniture.mobile.util.NetworkUtil.isOnline(this) ? View.GONE : View.VISIBLE);

        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return;
        networkCallback = new android.net.ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@androidx.annotation.NonNull android.net.Network network) {
                runOnUiThread(() -> {
                    if (binding != null) binding.tvOfflineBanner.setVisibility(View.GONE);
                });
            }
            @Override
            public void onLost(@androidx.annotation.NonNull android.net.Network network) {
                runOnUiThread(() -> {
                    if (binding != null && !com.unifurniture.mobile.util.NetworkUtil.isOnline(MainActivity.this))
                        binding.tvOfflineBanner.setVisibility(View.VISIBLE);
                });
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            android.net.ConnectivityManager cm =
                    (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null) {
                try { cm.unregisterNetworkCallback(networkCallback); } catch (IllegalArgumentException ignored) {}
            }
            networkCallback = null;
        }
    }

    private void setupCartBadge() {
        com.unifurniture.mobile.util.CartManager.getInstance().getCartCount().observe(this, count -> {
            var badge = binding.bottomNavigation.getOrCreateBadge(R.id.cartFragment);
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                badge.setVisible(false);
            }
        });

        refreshCart();
    }

    /**
     * Re-fetch the active cart for the current session into CartManager so the bottom-nav badge
     * stays correct after login/logout. This activity is no longer recreated when the session
     * changes (auth runs via {@link #authLauncher}), so the old onCreate fetch is no longer enough.
     * With no session (guest / just logged out) we clear the cart so a stale badge doesn't linger.
     */
    public void refreshCart() {
        com.unifurniture.mobile.util.SessionManager session =
                com.unifurniture.mobile.util.SessionManager.getInstance(this);
        String customerId = session.getCustomerId();
        String cartId = session.getCartId();
        if (customerId == null && cartId == null) {
            com.unifurniture.mobile.util.CartManager.getInstance().updateCart(null);
            return;
        }
        com.unifurniture.mobile.data.remote.ApiService api = com.unifurniture.mobile.data.remote.ApiClient.getInstance();
        api.getActiveCart(customerId, cartId).enqueue(new retrofit2.Callback<com.unifurniture.mobile.data.model.CartDto>() {
            @Override
            public void onResponse(retrofit2.Call<com.unifurniture.mobile.data.model.CartDto> call, retrofit2.Response<com.unifurniture.mobile.data.model.CartDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.unifurniture.mobile.util.CartManager.getInstance().updateCart(response.body());
                    session.saveCartId(response.body().id);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.unifurniture.mobile.data.model.CartDto> call, Throwable t) {}
        });
    }

    private void setupDraggableChatFab() {
        binding.fabChat.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    chatFabDragged = false;
                    chatFabDownRawX = event.getRawX();
                    chatFabDownRawY = event.getRawY();
                    chatFabDownX = view.getX();
                    chatFabDownY = view.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - chatFabDownRawX;
                    float deltaY = event.getRawY() - chatFabDownRawY;
                    if (!chatFabDragged && (Math.abs(deltaX) > CHAT_FAB_DRAG_THRESHOLD_PX
                            || Math.abs(deltaY) > CHAT_FAB_DRAG_THRESHOLD_PX)) {
                        chatFabDragged = true;
                    }
                    if (!chatFabDragged) {
                        return false;
                    }

                    float nextX = chatFabDownX + deltaX;
                    float nextY = chatFabDownY + deltaY;
                    view.setX(clampChatFabX(nextX, view));
                    view.setY(clampChatFabY(nextY, view));
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!chatFabDragged) {
                        view.performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return chatFabDragged;
                default:
                    return false;
            }
        });
    }

    private float clampChatFabX(float targetX, View fab) {
        float maxX = Math.max(CHAT_FAB_EDGE_MARGIN_PX,
                binding.getRoot().getWidth() - fab.getWidth() - CHAT_FAB_EDGE_MARGIN_PX);
        return Math.max(CHAT_FAB_EDGE_MARGIN_PX, Math.min(targetX, maxX));
    }

    private float clampChatFabY(float targetY, View fab) {
        float topLimit = CHAT_FAB_EDGE_MARGIN_PX + binding.tvOfflineBanner.getHeight();
        float bottomInset = binding.bottomNavigation.getVisibility() == View.VISIBLE
                ? binding.bottomNavigation.getHeight() + CHAT_FAB_EDGE_MARGIN_PX
                : CHAT_FAB_EDGE_MARGIN_PX;
        float maxY = Math.max(topLimit,
                binding.getRoot().getHeight() - fab.getHeight() - bottomInset);
        return Math.max(topLimit, Math.min(targetY, maxY));
    }

    private void syncBottomNavigationState(NavDestination destination) {
        if (destination == null) return;
        int tabId = getParentTabId(destination.getId());
        binding.bottomNavigation.setVisibility(shouldShowBottomNav(destination.getId()) ? View.VISIBLE : View.GONE);
        // Hide the floating chat button while the chat screen is open.
        binding.fabChat.setVisibility(destination.getId() == R.id.chatFragment ? View.GONE : View.VISIBLE);

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
        if (navController.popBackStack(destinationId, false)) {
            return true;
        }

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
