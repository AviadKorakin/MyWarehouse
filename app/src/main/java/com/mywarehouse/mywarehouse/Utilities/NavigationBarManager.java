package com.mywarehouse.mywarehouse.Utilities;

import android.content.Context;
import android.util.Log;
import android.view.Menu;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.R;

public class NavigationBarManager {

    private static final String TAG = "NavigationBarManager";
    private static NavigationBarManager instance;
    private String userRole;

    private NavigationBarManager(String role) {
        // Private constructor to prevent instantiation
        this.userRole = role;
    }

    public static NavigationBarManager getInstance(String role) {
        if (instance == null) {
            instance = new NavigationBarManager(role);
        }
        return instance;
    }
    public static NavigationBarManager getInstance() {
        return instance;
    }

    public void setupBottomNavigationView(BottomNavigationView bottomNavigationView, Context context) {
        Log.d(TAG, "Setting up BottomNavigationView for role: " + userRole);
        Menu menu = bottomNavigationView.getMenu();
        menu.clear();

        if (userRole.equals("Warehouse worker And Picker")) {
            menu.add(Menu.NONE, R.id.navigation_home, Menu.NONE, R.string.home)
                    .setIcon(R.drawable.ic_home);
            menu.add(Menu.NONE, R.id.navigation_orders, Menu.NONE, R.string.orders)
                    .setIcon(R.drawable.ic_orders);
            menu.add(Menu.NONE, R.id.navigation_reports, Menu.NONE, R.string.reports)
                    .setIcon(R.drawable.ic_reports);
            menu.add(Menu.NONE, R.id.navigation_inventory, Menu.NONE, R.string.inventory)
                    .setIcon(R.drawable.ic_inventory);
            menu.add(Menu.NONE, R.id.navigation_account, Menu.NONE, R.string.account)
                    .setIcon(R.drawable.ic_account);
        } else if (userRole.equals("Warehouse worker")) {
            menu.add(Menu.NONE, R.id.navigation_home, Menu.NONE, R.string.home)
                    .setIcon(R.drawable.ic_home);
            menu.add(Menu.NONE, R.id.navigation_reports, Menu.NONE, R.string.reports)
                    .setIcon(R.drawable.ic_reports);
            menu.add(Menu.NONE, R.id.navigation_inventory, Menu.NONE, R.string.inventory)
                    .setIcon(R.drawable.ic_inventory);
            menu.add(Menu.NONE, R.id.navigation_account, Menu.NONE, R.string.account)
                    .setIcon(R.drawable.ic_account);
        } else if (userRole.equals("Picker")) {
            menu.add(Menu.NONE, R.id.navigation_home, Menu.NONE, R.string.home)
                    .setIcon(R.drawable.ic_home);
            menu.add(Menu.NONE, R.id.navigation_orders, Menu.NONE, R.string.orders)
                    .setIcon(R.drawable.ic_orders);
            menu.add(Menu.NONE, R.id.navigation_inventory, Menu.NONE, R.string.inventory)
                    .setIcon(R.drawable.ic_inventory);
            menu.add(Menu.NONE, R.id.navigation_account, Menu.NONE, R.string.account)
                    .setIcon(R.drawable.ic_account);
        } else {
            Log.e(TAG, "Unknown role: " + userRole);
        }
    }
}
