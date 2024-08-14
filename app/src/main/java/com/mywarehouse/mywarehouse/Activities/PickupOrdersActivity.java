package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.PickupOrderAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebasePickupOrders;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PickupOrdersActivity extends AppCompatActivity {
    private AppCompatSpinner spinnerWarehouses;
    private RecyclerView recyclerViewOrders;
    private PickupOrderAdapter pickupOrderAdapter;
    private BottomNavigationView bottomNavigationView;
    private List<Order> orderList;
    private List<Warehouse> warehouseList;
    private Map<String, List<PickupItemWithImages>> orderPickupItemsMap;
    private Map<String, List<Item>> orderItemsMap;
    private Map<String, Item> cachedItemsMap;  // Cache for fetched items

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        fetchInitialData();
    }

    private void initViews() {
        spinnerWarehouses = findViewById(R.id.spinner_warehouses);
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        orderList = new ArrayList<>();
        warehouseList = new ArrayList<>();
        orderPickupItemsMap = new HashMap<>();
        orderItemsMap = new HashMap<>();
        cachedItemsMap = new HashMap<>();  // Initialize the cache

        pickupOrderAdapter = new PickupOrderAdapter(this, orderList, orderPickupItemsMap, orderItemsMap, "");

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(pickupOrderAdapter);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(PickupOrdersActivity.this, OrdersActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_orders);
    }

    private void fetchInitialData() {
        fetchWarehouses(() -> {
            fetchOrders();
            setupOrderListeners();
            setupItemListeners();
        });
    }

    private void fetchWarehouses(Runnable onComplete) {
        FirebasePickupOrders.fetchWarehouses(new FirebasePickupOrders.FetchCallback<Warehouse>() {
            @Override
            public void onSuccess(List<Warehouse> warehouses) {
                warehouseList.clear();
                warehouseList.addAll(warehouses);
                setupWarehouseSpinner();
                onComplete.run();
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

    private void fetchOrders() {
        FirebasePickupOrders.fetchOrders(new FirebasePickupOrders.FetchCallback<Order>() {
            @Override
            public void onSuccess(List<Order> orders) {
                orderList.clear();
                orderList.addAll(orders);
                fetchItemsForOrders();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error getting orders", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchItemsForOrders() {
        if (orderList.isEmpty()) return;

        for (Order order : orderList) {
            FirebasePickupOrders.fetchPickupItemsWithImages(order.getPickupItems(), new FirebasePickupOrders.PickupItemsCallback() {
                @Override
                public void onCallback(List<PickupItemWithImages> pickupItemsWithImages) {
                    orderPickupItemsMap.put(order.getOrderId(), pickupItemsWithImages);

                    // Fetch item details for each pickup item
                    List<Item> itemsList = new ArrayList<>();
                    for (PickupItemWithImages pickupItemWithImages : pickupItemsWithImages) {
                        String itemKey = pickupItemWithImages.getPickupItem().getBarcode() + "_" + pickupItemWithImages.getPickupItem().getName();
                        if (cachedItemsMap.containsKey(itemKey)) {
                            itemsList.add(cachedItemsMap.get(itemKey));
                            if (itemsList.size() == pickupItemsWithImages.size()) {
                                orderItemsMap.put(order.getOrderId(), itemsList);
                            }
                        } else {
                            FirebasePickupOrders.fetchItem(itemKey, item -> {
                                if (item != null) {
                                    cachedItemsMap.put(itemKey, item);  // Store in cache
                                    itemsList.add(item);
                                    if (itemsList.size() == pickupItemsWithImages.size()) {
                                        orderItemsMap.put(order.getOrderId(), itemsList);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void setupOrderListeners() {
        FirebasePickupOrders.listenToOrderChanges(new FirebasePickupOrders.OrdersListenerCallback() {
            @Override
            public void onOrderAdded(Order order) {
                orderList.add(order);
                pickupOrderAdapter.notifyItemInserted(orderList.size() - 1);
            }

            @Override
            public void onOrderModified(Order order) {
                int index = findOrderIndexById(order.getOrderId());
                if (index != -1) {
                    orderList.set(index, order);
                    pickupOrderAdapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onOrderRemoved(Order order) {
                int index = findOrderIndexById(order.getOrderId());
                if (index != -1) {
                    orderList.remove(index);
                    pickupOrderAdapter.notifyItemRemoved(index);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error listening to order changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupItemListeners() {
        FirebasePickupOrders.listenToItemChanges(item -> {
            String itemKey = item.getBarcode() + "_" + item.getName();
            cachedItemsMap.put(itemKey, item);  // Update the cache

            for (int i = 0; i < orderList.size(); i++) {
                List<Item> itemsList = orderItemsMap.get(orderList.get(i).getOrderId());
                if (itemsList != null) {
                    for (int j = 0; j < itemsList.size(); j++) {
                        if (itemsList.get(j).equals(item)) {
                            itemsList.set(j, item);
                        }
                    }
                }
            }
            pickupOrderAdapter.checkOrderAvailability();
        });
    }

    private int findOrderIndexById(String orderId) {
        for (int i = 0; i < orderList.size(); i++) {
            if (orderList.get(i).getOrderId().equals(orderId)) {
                return i;
            }
        }
        return -1;
    }


}