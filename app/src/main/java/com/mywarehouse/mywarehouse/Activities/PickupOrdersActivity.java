package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
    private ProgressBar progressBar;
    private List<Order> orderList;
    private List<Warehouse> warehouseList;
    private Map<String, List<PickupItemWithImages>> orderPickupItemsMap;
    private Map<String, List<Item>> orderItemsMap;
    private Map<String, Item> cachedItemsMap;
    private Map<String, List<String>> itemOrderMap; // Maps item keys to list of order IDs
    private Map<String, Boolean> itemListenerMap; // Maps item keys to whether a listener has been set up
    private int totalRegisteredOrders = 0;
    private int loadedOrders = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        showLoading();
        fetchWarehousesAndSetupListeners();
    }

    private void initViews() {
        spinnerWarehouses = findViewById(R.id.spinner_warehouses);
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);

        orderList = new ArrayList<>();
        warehouseList = new ArrayList<>();
        orderPickupItemsMap = new HashMap<>();
        orderItemsMap = new HashMap<>();
        cachedItemsMap = new HashMap<>();
        itemOrderMap = new HashMap<>();
        itemListenerMap = new HashMap<>();

        // Initialize the adapter early, but don't hide the loading indicator yet
        pickupOrderAdapter = new PickupOrderAdapter(this, orderList, orderPickupItemsMap, orderItemsMap, "NONE");
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

    private void fetchWarehousesAndSetupListeners() {
        FirebasePickupOrders.fetchWarehouses(new FirebasePickupOrders.FetchCallback<Warehouse>() {
            @Override
            public void onSuccess(List<Warehouse> warehouses) {
                warehouseList.clear();
                warehouseList.addAll(warehouses);
                setupWarehouseSpinner();
                fetchOrderCountAndInitializeListeners(); // Proceed to fetch orders count and setup listeners
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error getting warehouses", Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
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
                pickupOrderAdapter.checkOrderAvailability(); // Re-check availability on warehouse change
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void fetchOrderCountAndInitializeListeners() {
        FirebasePickupOrders.fetchRegisteredOrderCount(new FirebasePickupOrders.OrderCountCallback() {
            @Override
            public void onOrderCountFetched(int count) {
                totalRegisteredOrders = count;
                if (totalRegisteredOrders == 0) {
                    Toast.makeText(PickupOrdersActivity.this, "No orders to show", Toast.LENGTH_SHORT).show();
                    hideLoading(); // Hide loading if there are no registered orders
                } else {
                    setupOrderListeners(); // Set up the real-time listeners for orders
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Failed to fetch order count", Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void setupOrderListeners() {
        FirebasePickupOrders.listenToAllOrdersWithStatusFiltering(new FirebasePickupOrders.OrdersListenerCallback() {
            @Override
            public void onOrderAdded(Order order) {
                orderList.add(order);
                pickupOrderAdapter.notifyItemInserted(orderList.size() - 1);
                fetchItemsForOrder(order); // Fetch items for the newly added order
            }

            @Override
            public void onOrderModified(Order order) {
                int index = findOrderIndexById(order.getOrderId());
                if (index != -1) {
                    orderList.set(index, order);
                    pickupOrderAdapter.notifyItemChanged(index);
                    fetchItemsForOrder(order); // Fetch items again if the order is modified
                }
            }

            @Override
            public void onOrderRemoved(Order order) {
                int index = findOrderIndexById(order.getOrderId());
                if (index != -1) {
                    orderList.remove(index);
                    removeOrderFromItemMap(order);
                    orderPickupItemsMap.remove(order.getOrderId());
                    orderItemsMap.remove(order.getOrderId());
                    pickupOrderAdapter.notifyItemRemoved(index);
                    checkIfAllOrdersLoaded();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PickupOrdersActivity.this, "Error listening to order changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void fetchItemsForOrder(Order order) {
        FirebasePickupOrders.fetchPickupItemsWithImages(order.getPickupItems(), pickupItemsWithImages -> {
            orderPickupItemsMap.put(order.getOrderId(), pickupItemsWithImages);

            // Fetch item details for each pickup item
            List<Item> itemsList = new ArrayList<>();
            for (PickupItemWithImages pickupItemWithImages : pickupItemsWithImages) {
                String itemKey = pickupItemWithImages.getPickupItem().getBarcode() + "_" + pickupItemWithImages.getPickupItem().getName();
                addOrderToItemMap(itemKey, order.getOrderId());

                if (cachedItemsMap.containsKey(itemKey)) {
                    itemsList.add(cachedItemsMap.get(itemKey));
                    if (itemsList.size() == pickupItemsWithImages.size()) {
                        orderItemsMap.put(order.getOrderId(), itemsList);
                        checkIfAllOrdersLoaded();
                    }
                } else {
                    FirebasePickupOrders.fetchItem(itemKey, item -> {
                        if (item != null) {
                            cachedItemsMap.put(itemKey, item);  // Store in cache
                            itemsList.add(item);
                            if (itemsList.size() == pickupItemsWithImages.size()) {
                                orderItemsMap.put(order.getOrderId(), itemsList);
                                checkIfAllOrdersLoaded();
                            }
                        }
                    });
                }
            }

            // Set up listeners for each item in the order
            setupItemListenersForOrder(order);
        });
    }

    private void setupItemListenersForOrder(Order order) {
        List<PickupItemWithImages> pickupItemsWithImagesList = orderPickupItemsMap.get(order.getOrderId());
        if (pickupItemsWithImagesList != null) {
            for (PickupItemWithImages pickupItemWithImages : pickupItemsWithImagesList) {
                String itemKey = pickupItemWithImages.getPickupItem().getBarcode() + "_" + pickupItemWithImages.getPickupItem().getName();
                if (!itemListenerMap.containsKey(itemKey)) { // Check if a listener is already set up
                    itemListenerMap.put(itemKey, true); // Mark listener as set up

                    FirebasePickupOrders.listenToItemChanges(itemKey, item -> {
                        if (item != null) {
                            // Update the cached item
                            cachedItemsMap.put(itemKey, item);

                            // Update the orders with the new item data
                            pickupOrderAdapter.updateItemInOrder(itemKey, item);

                        } else {
                            // If the item was removed, remove it from the cache and the orders
                            cachedItemsMap.remove(itemKey);
                            pickupOrderAdapter.updateItemInOrder(itemKey, null);
                        }

                        resetSpinnerSelection(); // Reset spinner to "NONE"
                    });
                }
            }
        }
    }


    private void addOrderToItemMap(String itemKey, String orderId) {
        List<String> orderIds = itemOrderMap.getOrDefault(itemKey, new ArrayList<>());
        orderIds.add(orderId);
        itemOrderMap.put(itemKey, orderIds);
    }

    private void removeOrderFromItemMap(Order order) {
        List<PickupItemWithImages> pickupItemsWithImagesList = orderPickupItemsMap.get(order.getOrderId());
        if (pickupItemsWithImagesList != null) {
            for (PickupItemWithImages pickupItemWithImages : pickupItemsWithImagesList) {
                String itemKey = pickupItemWithImages.getPickupItem().getBarcode() + "_" + pickupItemWithImages.getPickupItem().getName();
                List<String> orderIds = itemOrderMap.get(itemKey);
                if (orderIds != null) {
                    orderIds.remove(order.getOrderId());
                    if (orderIds.isEmpty()) {
                        itemOrderMap.remove(itemKey);
                    }
                }
            }
        }
    }


    private void resetSpinnerSelection() {
        spinnerWarehouses.setSelection(0);
    }

    private int findOrderIndexById(String orderId) {
        for (int i = 0; i < orderList.size(); i++) {
            if (orderList.get(i).getOrderId().equals(orderId)) {
                return i;
            }
        }
        return -1;
    }

    private void checkIfAllOrdersLoaded() {
        loadedOrders++;
        if (loadedOrders >= totalRegisteredOrders) {
            hideLoading(); // All registered orders are loaded, hide the loading spinner
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewOrders.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewOrders.setVisibility(View.VISIBLE);
        spinnerWarehouses.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebasePickupOrders.removeAllListeners(); // Remove listeners when the activity is destroyed
    }
}
