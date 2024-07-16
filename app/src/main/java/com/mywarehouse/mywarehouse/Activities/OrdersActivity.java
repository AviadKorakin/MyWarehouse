package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.OrderAdapter;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private Button buttonMyOrders;
    private BottomNavigationView bottomNavigationView;
    private Intent intent = null;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        db = FirebaseFirestore.getInstance();

        findViews();

        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, this::onOrderItemClicked);

        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(orderAdapter);

        buttonMyOrders.setOnClickListener(v -> {
            // Handle My Orders button click
            Toast.makeText(OrdersActivity.this, "My Orders clicked", Toast.LENGTH_SHORT).show();
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                intent = new Intent(OrdersActivity.this, HomeActivity.class);
            } else if (id == R.id.navigation_inventory) {
                intent = new Intent(OrdersActivity.this, InventoryActivity.class);
            } else if (id == R.id.navigation_reports) {
                intent = new Intent(OrdersActivity.this, ReportsActivity.class);
            } else if (id == R.id.navigation_orders) {
                return true;
            } else if (id == R.id.navigation_account) {
                intent = new Intent(OrdersActivity.this, AccountActivity.class);
            }

            if (intent != null) {
                bottomNavigationView.postOnAnimation(() -> {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    startActivity(intent);
                    finish();
                });
            }
            return true;
        });

        fetchOrders();
    }

    private void findViews() {
        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        buttonMyOrders = findViewById(R.id.button_my_orders);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void fetchOrders() {
        db.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            orderList.add(order);
                        }
                        orderAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(OrdersActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onOrderItemClicked(Order order) {
        // Handle order item click
        Toast.makeText(OrdersActivity.this, "Order clicked: " + order.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
