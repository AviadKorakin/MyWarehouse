package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.MyOrdersAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseMyOrders;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private MyOrdersAdapter myOrdersAdapter;
    private List<Order> orderList;
    private Map<String, Order> orderMap;  // Map to track orders by ID
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;  // Loading indicator
    private int totalOrderCount = 0;  // Total number of orders
    private int loadedOrderCount = 0;  // Number of orders loaded so far
    private boolean isInitialLoadComplete = false;  // Track if the initial load is complete

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        showLoading();  // Show loading indicator
        setupListeners();
    }

    private void initViews() {
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);

        orderList = new ArrayList<>();
        orderMap = new HashMap<>();
        myOrdersAdapter = new MyOrdersAdapter(this, orderList);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(myOrdersAdapter);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(MyOrdersActivity.this, OrdersActivity.class);
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

    private void setupListeners() {
        FirebaseMyOrders.listenToUserOrders(MyUser.getInstance().getUser().getUserId(), new FirebaseMyOrders.OrdersCallback() {
            @Override
            public void onListCleared() {
                orderList.clear();
                orderMap.clear();
                myOrdersAdapter.notifyDataSetChanged(); // Notify adapter that data has been cleared
            }

            @Override
            public void onOrderAdded(Order order) {
                orderList.add(order);
                orderMap.put(order.getOrderId(), order);
                loadedOrderCount++;
                myOrdersAdapter.notifyItemInserted(orderList.size() - 1);
                checkInitialLoadComplete();
            }

            @Override
            public void onOrderModified(Order updatedOrder) {
                int index = findOrderIndexById(updatedOrder.getOrderId());
                if (index != -1) {
                    orderList.set(index, updatedOrder);
                    orderMap.put(updatedOrder.getOrderId(), updatedOrder);
                    myOrdersAdapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onOrderRemoved(String orderId) {
                int index = findOrderIndexById(orderId);
                if (index != -1) {
                    orderList.remove(index);
                    orderMap.remove(orderId);
                    loadedOrderCount++;
                    myOrdersAdapter.notifyItemRemoved(index);
                    checkInitialLoadComplete();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyOrdersActivity.this, "Error listening to orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading();  // Hide loading on error
            }
        }, new FirebaseMyOrders.OrderCountCallback() {
            @Override
            public void onOrderCountFetched(int count) {
                totalOrderCount = count;
                if(count==0)
                {
                    Toast.makeText(MyOrdersActivity.this, "No orders to show", Toast.LENGTH_SHORT).show();
                }
                checkInitialLoadComplete();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyOrdersActivity.this, "Failed to fetch total order count", Toast.LENGTH_SHORT).show();
                hideLoading();  // Hide loading on failure
            }
        });
    }

    private int findOrderIndexById(String orderId) {
        for (int i = 0; i < orderList.size(); i++) {
            if (orderList.get(i).getOrderId().equals(orderId)) {
                return i;
            }
        }
        return -1;  // Return -1 if the order is not found
    }

    private void checkInitialLoadComplete() {
        if (loadedOrderCount >= totalOrderCount && !isInitialLoadComplete) {
            isInitialLoadComplete = true;
            hideLoading();  // Hide the loading screen after the initial data load is complete
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewOrders.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewOrders.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseMyOrders.removeAllListeners(); // Remove listeners when the activity is destroyed
    }
}
