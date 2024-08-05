package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Adapters.PickupOrderAdapter;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebasePickupOrders;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.List;

public class PickupOrdersActivity extends AppCompatActivity {

    private AppCompatSpinner spinnerWarehouses;
    private RecyclerView recyclerViewOrders;
    private PickupOrderAdapter pickupOrderAdapter;
    private BottomNavigationView bottomNavigationView;
    private AppCompatImageButton refreshButton;
    private List<Order> orderList;
    private FirebaseFirestore db;
    private List<Warehouse> warehouseList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        spinnerWarehouses = findViewById(R.id.spinner_warehouses);
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();
        orderList = new ArrayList<>();
        warehouseList = new ArrayList<>();
        pickupOrderAdapter = new PickupOrderAdapter(this, orderList, "");

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(pickupOrderAdapter);
        refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> refreshOrders());
        db = FirebaseFirestore.getInstance();

        fetchWarehouses();
        setupWarehouseSpinner();
        fetchOrders();
    }

    private void fetchWarehouses() {
        FirebasePickupOrders.fetchWarehouses(db, new FirebasePickupOrders.FetchCallback<Warehouse>() {
            @Override
            public void onSuccess(List<Warehouse> warehouses) {
                warehouseList.clear();
                warehouseList.addAll(warehouses);
                setupWarehouseSpinner();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error getting warehouses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWarehouseSpinner() {
        List<String> warehouseNames = new ArrayList<>();
        warehouseNames.add("NONE");
        for (Warehouse warehouse : warehouseList) {
            warehouseNames.add(warehouse.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, warehouseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWarehouses.setAdapter(adapter);

        spinnerWarehouses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedWarehouse = warehouseNames.get(position);
                pickupOrderAdapter.setSelectedWarehouse(selectedWarehouse);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }
    private void refreshOrders() {
        orderList.clear(); // Clear the current list to avoid duplicates
        spinnerWarehouses.setSelection(0);
        fetchOrders(); // Fetch the latest orders
    }

    private void fetchOrders() {
        FirebasePickupOrders.fetchOrders(db, new FirebasePickupOrders.FetchCallback<Order>() {
            @Override
            public void onSuccess(List<Order> orders) {
                orderList.clear();
                orderList.addAll(orders);
                pickupOrderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error getting orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
