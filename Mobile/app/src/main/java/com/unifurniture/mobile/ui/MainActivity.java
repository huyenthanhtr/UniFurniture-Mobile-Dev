package com.unifurniture.mobile.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.unifurniture.mobile.R;
import com.unifurniture.mobile.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

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
        NavController navController = navHostFragment.getNavController();

        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            try {
                navController.navigate(item.getItemId(), null, navOptions);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Menu menu = binding.bottomNavigation.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem menuItem = menu.getItem(i);
                if (menuItem.getItemId() == destination.getId()) {
                    menuItem.setChecked(true);
                    break;
                }
            }
        });
    }
}
