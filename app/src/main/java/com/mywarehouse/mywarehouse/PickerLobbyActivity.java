package com.mywarehouse.mywarehouse;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.mywarehouse.mywarehouse.Activities.AccountActivity;

public class PickerLobbyActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker_lobby);

        findViews();

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    Toast.makeText(PickerLobbyActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_orders) {
                    Toast.makeText(PickerLobbyActivity.this, "Orders selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_inventory) {
                    Toast.makeText(PickerLobbyActivity.this, "Inventory selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.navigation_account) {
                    Intent intent = new Intent(PickerLobbyActivity.this, AccountActivity.class);
                    startActivity(intent);
                    finish();
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
