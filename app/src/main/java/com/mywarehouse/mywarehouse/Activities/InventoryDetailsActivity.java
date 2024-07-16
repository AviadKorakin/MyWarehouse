package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mywarehouse.mywarehouse.Adapters.InventoryAdapter;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryDetailsActivity extends AppCompatActivity {

    private TextInputEditText searchInput;
    private RecyclerView recyclerViewItems;
    private BottomNavigationView bottomNavigationView;
    private InventoryAdapter inventoryAdapter;
    private List<Item> itemList;
    private Map<String, Item> itemMap; // Map to store items with document ID as the key
    private FirebaseFirestore db;
    private Intent intent;
    private Gson gson;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_details);

        db = FirebaseFirestore.getInstance();
        gson = new Gson();

        findViews();
        initViews();
        setupSearch();
        setupNavigationBar();
    }

    private void findViews() {
        searchInput = findViewById(R.id.search_input);
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void initViews() {
        itemList = new ArrayList<>();
        itemMap = new HashMap<>();
        inventoryAdapter = new InventoryAdapter(itemList);
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(inventoryAdapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    searchItems(query);
                } else {
                    itemList.clear();
                    itemMap.clear();
                    inventoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });
    }

    private void searchItems(String query) {
        db.collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        itemMap.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Item item = document.toObject(Item.class);
                            String documentId = document.getId();

                            if ((item.getBarcode().contains(query) || item.getName().contains(query)) && !itemMap.containsKey(documentId)) {
                                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                                ArrayList<String> imageUrls = gson.fromJson(gson.toJson(document.get("imageUrls")), listType);
                                item.setImageUrls(imageUrls != null ? imageUrls : new ArrayList<>());
                                itemMap.put(documentId, item);
                            }
                        }
                        updateItemList();
                    } else {
                        Toast.makeText(this, "Error fetching items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateItemList() {
        itemList.clear();
        itemList.addAll(itemMap.values());
        inventoryAdapter.notifyDataSetChanged();
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                intent = null;
                if (id == R.id.navigation_home) {
                    intent = new Intent(InventoryDetailsActivity.this, HomeActivity.class);
                } else if (id == R.id.navigation_account) {
                    intent = new Intent(InventoryDetailsActivity.this, AccountActivity.class);
                } else if (id == R.id.navigation_reports) {
                    intent = new Intent(InventoryDetailsActivity.this, ReportsActivity.class);
                } else if (id == R.id.navigation_orders) {
                    intent = new Intent(InventoryDetailsActivity.this, OrdersActivity.class);
                } else if (id == R.id.navigation_inventory) {
                    intent = new Intent(InventoryDetailsActivity.this, InventoryActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    startActivity(intent);
                    finish();
                }
                return true;
            }
        });
    }
}
