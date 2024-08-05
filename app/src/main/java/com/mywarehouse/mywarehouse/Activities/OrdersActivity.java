package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

public class OrdersActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();

        MaterialButton buttonAddOrder = findViewById(R.id.button_add_order);
        MaterialButton buttonPickUpAnOrder = findViewById(R.id.button_pickup_orders);
        MaterialButton buttonMyPickUps = findViewById(R.id.button_my_pickups);
        MaterialButton buttonMyOrders = findViewById(R.id.button_my_orders);

        buttonAddOrder.setOnClickListener(v -> openAddOrderActivity());
        buttonPickUpAnOrder.setOnClickListener(v -> openPickUpOrdersActivity());
        buttonMyPickUps.setOnClickListener(v -> openMyPickUpsActivity());
        buttonMyOrders.setOnClickListener(v -> openMyOrdersActivity());
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_orders);

        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }

    private void openAddOrderActivity() {
        Intent intent = new Intent(OrdersActivity.this, AddOrderActivity.class);
        startActivity(intent);
        finish();
    }

    private void openPickUpOrdersActivity() {
         Intent intent = new Intent(OrdersActivity.this, PickupOrdersActivity.class);
         startActivity(intent);
        finish();
    }


    private void openMyPickUpsActivity() {
        Intent intent = new Intent(OrdersActivity.this, MyPickUpsActivity.class);
        startActivity(intent);
        finish();
    }

    private void openMyOrdersActivity() {
         Intent intent = new Intent(OrdersActivity.this, MyOrdersActivity.class);
         startActivity(intent);
         finish();
    }
}
