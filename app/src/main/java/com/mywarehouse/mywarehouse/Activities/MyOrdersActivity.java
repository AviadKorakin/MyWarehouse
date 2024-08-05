package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private MyOrdersAdapter myOrdersAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;
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

        db = FirebaseFirestore.getInstance();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();

        fetchUserOrders();
    }

    private void fetchUserOrders() {
        String userId = MyUser.getInstance().getDocumentId();

        FirebaseMyOrders.fetchUserOrders(db, userId, new  FirebaseMyOrders.OrdersCallback() {
            @Override
            public void onOrdersFetched(List<Order> orders) {
                orderList.clear();
                orderList.addAll(orders);
                myOrdersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyOrdersActivity.this, "Error getting user orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }
}
