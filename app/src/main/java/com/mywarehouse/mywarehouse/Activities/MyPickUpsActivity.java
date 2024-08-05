package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.MyPickupsAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseMyPickups;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyPickUpsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPickups;
    private MyPickupsAdapter myPickupsAdapter;
    private List<Order> pickupList;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pick_ups);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        recyclerViewPickups = findViewById(R.id.recycler_view_pickups);
        pickupList = new ArrayList<>();
        myPickupsAdapter = new MyPickupsAdapter(this, pickupList);

        recyclerViewPickups.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPickups.setAdapter(myPickupsAdapter);

        db = FirebaseFirestore.getInstance();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();

        fetchUserPickups();
    }

    private void fetchUserPickups() {
        String userId = MyUser.getInstance().getDocumentId();

        FirebaseMyPickups.fetchUserPickups(db, userId, new FirebaseMyPickups.PickupsCallback() {
            @Override
            public void onPickupsFetched(List<Order> pickups) {
                pickupList.clear();
                pickupList.addAll(pickups);
                myPickupsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyPickUpsActivity.this, "Error getting user pickups: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }
}
