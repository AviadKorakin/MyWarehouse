package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Firebase;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.InventoryAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseInventory;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

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
    private AppCompatImageButton buttonScanBarcode;
    private Map<String, Item> itemMap; // Map to store items with document ID as the key
    private Map<String, List<Item>> queryCache; // Map to store query results

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            searchInput.setText(result.getContents());
        }
    });

    private final ActivityResultLauncher<Intent> updateItemLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_details);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        findViews();
        initViews();
        setupSearch();
        setupNavigationBar();
        setupRealtimeListener();  // Set up the real-time listener
    }

    private void findViews() {
        searchInput = findViewById(R.id.search_input);
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
    }

    private void initViews() {
        itemList = new ArrayList<>();
        itemMap = new HashMap<>();
        queryCache = new HashMap<>();
        inventoryAdapter = new InventoryAdapter(itemList, item -> {
            if(!item.isOnUpdate()) {
                Intent intent = new Intent(InventoryDetailsActivity.this, UpdateItemBundledActivity.class);
                intent.putExtra("item", item);
                updateItemLauncher.launch(intent);
            }
            else
            {
                Toast.makeText(InventoryDetailsActivity.this, "This item is currently being updated by someone else.", Toast.LENGTH_SHORT).show();
            }
        });


        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(inventoryAdapter);
        buttonScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });
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

                if (queryCache.containsKey(query)) {
                    updateItemList(queryCache.get(query));
                } else {
                    searchItems(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(InventoryDetailsActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    private void searchItems(String query) {
        List<Item> results = addToQueryCache(query);
        updateItemList(results);
    }

    private List<Item> addToQueryCache(String query) {
        List<Item> results = new ArrayList<>();
        if (itemMap.isEmpty()) return results;
        for (Item item : itemMap.values()) {
            if (item.getBarcode().contains(query) || item.getName().contains(query)) {
                results.add(item);
            }
        }
        queryCache.put(query, results);
        return results;
    }

    private void updateItemList(List<Item> items) {
        inventoryAdapter.updateItems(items);
    }

    private void setupRealtimeListener() {
        FirebaseInventory.listenToItems(new FirebaseInventory.InventoryCallback() {
            @Override
            public void onCallback(List<Item> itemList) {
                itemMap.clear();
                for (Item item : itemList) {
                    itemMap.put(item.getBarcode() + "_" + item.getName(), item);
                }
                queryCache.clear(); // Clear the query cache
                updateItemList(itemList);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(InventoryDetailsActivity.this, "Error listening to items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_inventory);
    }
}