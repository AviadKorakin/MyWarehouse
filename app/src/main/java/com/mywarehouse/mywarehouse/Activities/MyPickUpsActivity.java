package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.util.ArrayList;
import java.util.List;

public class MyPickUpsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPickups;
    private MyPickupsAdapter myPickupsAdapter;
    private List<Order> pickupList;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> pickUpActivityLauncher;
    private int totalPickupCount = -1;
    private int loadedPickupCount = 0;
    private boolean isInitialLoadComplete = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pick_ups);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        showLoading();
        setupListeners();

        pickUpActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> fetchUserPickupsWithListeners()
        );

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(MyPickUpsActivity.this, OrdersActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initViews() {
        recyclerViewPickups = findViewById(R.id.recycler_view_pickups);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);

        pickupList = new ArrayList<>();
        myPickupsAdapter = new MyPickupsAdapter(this, pickupList, this::launchPickUpActivity);

        recyclerViewPickups.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPickups.setAdapter(myPickupsAdapter);
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_orders);
    }

    private void setupListeners() {
        fetchUserPickupsWithListeners();
    }

    private void launchPickUpActivity(Order order) {
        Intent intent = new Intent(this, ShowPickUpActivity.class);
        intent.putExtra("order", order);
        pickUpActivityLauncher.launch(intent);
    }

    private void fetchUserPickupsWithListeners() {
        String userId = MyUser.getInstance().getUser().getUserId();

        FirebaseMyPickups.listenToUserPickups(userId, new FirebaseMyPickups.PickupsCallback() {
            @Override
            public void onListCleared() {
                pickupList.clear();
                myPickupsAdapter.notifyDataSetChanged(); // Notify adapter that data has been cleared
            }

            @Override
            public void onPickupAdded(Order pickup) {
                pickupList.add(pickup);
                loadedPickupCount++;
                myPickupsAdapter.notifyItemInserted(pickupList.size() - 1);
                checkInitialLoadComplete();
            }

            @Override
            public void onPickupModified(Order pickup) {
                int index = findPickupIndexById(pickup.getOrderId());
                if (index != -1) {
                    pickupList.set(index, pickup);
                    myPickupsAdapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onPickupRemoved(String pickupId) {
                int index = findPickupIndexById(pickupId);
                if (index != -1) {
                    pickupList.remove(index);
                    loadedPickupCount--;
                    if (pickupList.isEmpty()) {
                        myPickupsAdapter.notifyDataSetChanged();
                    } else {
                        myPickupsAdapter.notifyItemRemoved(index);
                    }
                    checkInitialLoadComplete();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyPickUpsActivity.this, "Error getting user pickups: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        }, new FirebaseMyPickups.PickupCountCallback() {
            @Override
            public void onPickupCountFetched(int count) {
                totalPickupCount = count;
                checkInitialLoadComplete();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MyPickUpsActivity.this, "Error getting pickup count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private int findPickupIndexById(String pickupId) {
        for (int i = 0; i < pickupList.size(); i++) {
            if (pickupList.get(i).getOrderId().equals(pickupId)) {
                return i;
            }
        }
        return -1;
    }

    private void checkInitialLoadComplete() {
        if (totalPickupCount!= -1 && loadedPickupCount >= totalPickupCount && !isInitialLoadComplete) {
            isInitialLoadComplete = true;
            hideLoading();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewPickups.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewPickups.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseMyPickups.removeAllListeners();
    }
}
