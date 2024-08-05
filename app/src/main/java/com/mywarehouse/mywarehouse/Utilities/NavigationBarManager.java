package com.mywarehouse.mywarehouse.Utilities;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Activities.AccountActivity;
import com.mywarehouse.mywarehouse.Activities.HomeActivity;
import com.mywarehouse.mywarehouse.Activities.InventoryActivity;
import com.mywarehouse.mywarehouse.Activities.OrdersActivity;
import com.mywarehouse.mywarehouse.Activities.ReportsActivity;

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

        if (userRole.equals("Warehouse worker and picker")) {
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

    public void setNavigation(BottomNavigationView bottomNavigationView, Context context, int selected) {
        bottomNavigationView.setSelectedItemId(selected);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                intent = new Intent(context, HomeActivity.class);
            } else if (id == R.id.navigation_account) {
                intent = new Intent(context, AccountActivity.class);
            } else if (id == R.id.navigation_reports) {
                intent = new Intent(context, ReportsActivity.class);
            } else if (id == R.id.navigation_orders) {
                intent = new Intent(context, OrdersActivity.class);
            } else if (id == R.id.navigation_inventory) {
                intent = new Intent(context, InventoryActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                context.startActivity(intent);
                if (context instanceof AppCompatActivity) {
                    ((AppCompatActivity) context).finish();
                }
            }
            return true;
        });
    }
}
