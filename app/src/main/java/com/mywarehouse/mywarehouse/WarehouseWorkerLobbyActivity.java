package com.mywarehouse.mywarehouse;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.mywarehouse.mywarehouse.R;

public class WarehouseWorkerLobbyActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_worker);
        findViews();

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    Toast.makeText(WarehouseWorkerLobbyActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_inventory) {
                    Toast.makeText(WarehouseWorkerLobbyActivity.this, "Inventory selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_reports) {
                    Toast.makeText(WarehouseWorkerLobbyActivity.this, "Reports selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_account) {
                    Toast.makeText(WarehouseWorkerLobbyActivity.this, "Account selected", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
    }

    private void findViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }
}
