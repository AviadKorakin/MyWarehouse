package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.MyOrdersAdapter;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Firebase.FirebaseMyOrders;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private MyOrdersAdapter myOrdersAdapter;
    private List<Order> orderList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        orderList = new ArrayList<>();
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
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();

        setupRealtimeOrderListener();
    }

    private void setupRealtimeOrderListener() {
        String userId = MyUser.getInstance().getUser().getUserId();

        FirebaseMyOrders.listenToUserOrders(userId, new FirebaseMyOrders.OrdersCallback() {
            @Override
            public void onOrdersFetched(List<Order> orders) {
                orderList.clear();
                orderList.addAll(orders);
                sortOrders();
                myOrdersAdapter.notifyDataSetChanged();

                // Set up listeners for each order document
                listenToOrderChanges(orders);
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private void listenToOrderChanges(List<Order> orders) {
        for (Order order : orders) {
            FirebaseMyOrders.listenToOrderChanges(order.getOrderId(), new FirebaseMyOrders.OrderUpdateCallback() {
                @Override
                public void onOrderUpdated(Order updatedOrder) {
                    // Update the specific order in the list
                    int index = orderList.indexOf(order);
                    if (index != -1) {
                        orderList.set(index, updatedOrder);
                        sortOrders();
                        myOrdersAdapter.notifyItemChanged(index);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(MyOrdersActivity.this, "Error updating order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sortOrders() {
        Collections.sort(orderList, new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                int priorityComparison = getOrderPriority(o1.getStatus()) - getOrderPriority(o2.getStatus());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return o1.getOrderDate().compareTo(o2.getOrderDate());
            }

            private int getOrderPriority(OrderType status) {
                switch (status) {
                    case REGISTERED:
                        return 1;
                    case IN_PROGRESS:
                        return 2;
                    case TRANSACTIONS_NEEDED:
                        return 3;
                    case PICKED_UP:
                        return 4;
                    case COMPLETED:
                        return 5;
                    default:
                        return 6;
                }
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_orders);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseForAdapters.removeAllItemChangeListeners();// Remove listeners when activity is destroyed
    }
}
