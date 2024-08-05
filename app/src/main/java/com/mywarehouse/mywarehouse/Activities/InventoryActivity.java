package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

public class InventoryActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Intent intent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        findViews();
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_inventory);

        findViewById(R.id.button_add_item).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, AddItemActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.button_add_warehouse).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, AddNewWarehouseActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.button_update_item).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, UpdateItemActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.button_warehouse_map).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, WarehouseMapActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.button_inventory).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, InventoryDetailsActivity.class);
            startActivity(intent);
            finish();
        });


        findViewById(R.id.button_request).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, RequestsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void findViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }
}
