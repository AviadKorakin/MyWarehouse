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

        findViews();
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);

        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_inventory) {
                Toast.makeText(InventoryActivity.this, "Inventory selected", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.navigation_account) {
                intent = new Intent(InventoryActivity.this, AccountActivity.class);
            } else if (id == R.id.navigation_reports) {
                intent = new Intent(InventoryActivity.this, ReportsActivity.class);
            } else if (id == R.id.navigation_orders) {
                intent = new Intent(InventoryActivity.this, OrdersActivity.class);
            } else if (id == R.id.navigation_home) {
                intent = new Intent(InventoryActivity.this, HomeActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                startActivity(intent);
                finish();
            }
            return true;
        });

        findViewById(R.id.button_add_item).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, AddItemActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_add_warehouse).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, AddNewWarehouseActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_update_item).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, UpdateItemActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_warehouse_map).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, WarehouseMapActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button_inventory).setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, InventoryDetailsActivity.class);
            startActivity(intent);
        });
    }

    private void findViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }
}
