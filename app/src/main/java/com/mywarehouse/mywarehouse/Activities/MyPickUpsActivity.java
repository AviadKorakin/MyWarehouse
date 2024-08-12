package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.MyPickupsAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Firebase.FirebaseMyPickups;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.List;

public class MyPickUpsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPickups;
    private MyPickupsAdapter myPickupsAdapter;
    private List<Order> pickupList;
    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<Intent> pickUpActivityLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pick_ups);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        recyclerViewPickups = findViewById(R.id.recycler_view_pickups);
        pickupList = new ArrayList<>();
        myPickupsAdapter = new MyPickupsAdapter(this, pickupList, this::launchPickUpActivity);

        recyclerViewPickups.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPickups.setAdapter(myPickupsAdapter);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();

        fetchUserPickupsWithListeners();

        pickUpActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> fetchUserPickupsWithListeners()
        );
    }

    private void launchPickUpActivity(Order order) {
        Intent intent = new Intent(this, ShowPickUpActivity.class);
        intent.putExtra("order", order);
        pickUpActivityLauncher.launch(intent);
    }

    private void fetchUserPickupsWithListeners() {
        String userId = MyUser.getInstance().getUser().getUserId();

        FirebaseMyPickups.fetchUserPickupsWithListeners(userId, new FirebaseMyPickups.PickupsCallback() {
            @Override
            public void onPickupsFetched(List<Order> pickups) {
                pickupList.clear();
                pickupList.addAll(pickups);
                myPickupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onOrderUpdated(Order order) {
                int index = findOrderIndexById(order.getOrderId());
                if (index != -1) {
                    pickupList.set(index, order);
                    myPickupsAdapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onOrderRemoved(String orderId) {
                int index = findOrderIndexById(orderId);
                if (index != -1) {
                    pickupList.remove(index);
                    if (pickupList.isEmpty()) {
                        myPickupsAdapter.notifyDataSetChanged();
                    } else {
                        myPickupsAdapter.notifyItemRemoved(index);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyPickUpsActivity.this, "Error getting user pickups: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int findOrderIndexById(String orderId) {
        for (int i = 0; i < pickupList.size(); i++) {
            if (pickupList.get(i).getOrderId().equals(orderId)) {
                return i;
            }
        }
        return -1;
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseMyPickups.removeAllListeners();
        FirebaseForAdapters.removeAllItemChangeListeners();// Remove listeners when activity is destroyed
    }
}
