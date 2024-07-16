package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Intent intent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViews();

        // Setup the bottom navigation view based on the user role
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);

        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                 intent = null;
                if (id == R.id.navigation_home) {
                    Toast.makeText(HomeActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.navigation_account) {
                    intent = new Intent(HomeActivity.this, AccountActivity.class);
                } else if (id == R.id.navigation_reports) {
                    intent = new Intent(HomeActivity.this, ReportsActivity.class);
                } else if (id == R.id.navigation_orders) {
                    intent = new Intent(HomeActivity.this, OrdersActivity.class);
                } else if (id == R.id.navigation_inventory) {
                    intent = new Intent(HomeActivity.this, InventoryActivity.class);
                }

                if (intent != null) {

                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            startActivity(intent);
                            finish();

                }
                return true;
            }
        });
    }
    private void findViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }
}