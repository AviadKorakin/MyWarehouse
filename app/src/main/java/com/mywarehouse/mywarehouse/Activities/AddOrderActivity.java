package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.ItemOrderAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseAddOrder;
import com.mywarehouse.mywarehouse.Models.ItemOrder;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.List;

public class AddOrderActivity extends AppCompatActivity {

    private TextInputEditText searchInput;
    private AppCompatTextView inCartCounter;
    private RecyclerView recyclerViewItems;
    private BottomNavigationView bottomNavigationView;
    private ItemOrderAdapter itemOrderAdapter;
    private List<ItemOrder> itemOrderList;
    private AppCompatImageButton buttonScanBarcode, buttonCart;
    private FirebaseFirestore db;
    private int totalSelectedItems = 0;
    private boolean shouldCancelFetch = false;
    private Intent intent = null;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            searchInput.setText(result.getContents());
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        db = FirebaseFirestore.getInstance();

        searchInput = findViewById(R.id.search_input);
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
        buttonCart = findViewById(R.id.cart_button);
        inCartCounter = findViewById(R.id.incart_number);
        updateTotalSelectedItems(0);
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        itemOrderList = new ArrayList<>();
        itemOrderAdapter = new ItemOrderAdapter(this, itemOrderList, this::updateTotalSelectedItems);
        recyclerViewItems.setAdapter(itemOrderAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });

        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            fetchData();
            resetInCartCounter();
        });

        buttonScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan a barcode");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        buttonCart.setOnClickListener(v -> {
            if (totalSelectedItems > 0) {
                Intent intent = new Intent(AddOrderActivity.this, CheckoutActivity.class);
                ArrayList<ItemOrder> selectedItems = getSelectedItems();
                intent.putParcelableArrayListExtra("selectedItems", selectedItems);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            }
        });

        fetchData();

        // Setup navigation bar
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);
    }

    private void fetchData() {
        if (shouldCancelFetch) {
            return; // Stop the fetch operation if the flag is set
        }

        FirebaseAddOrder.fetchItems(db, new FirebaseAddOrder.ItemsCallback() {
            @Override
            public void onItemsFetched(List<ItemOrder> items) {
                itemOrderList.clear();
                itemOrderList.addAll(items);
                itemOrderAdapter.updateItems(itemOrderList);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AddOrderActivity.this, "Error fetching items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterItems(String query) {
        List<ItemOrder> filteredList = new ArrayList<>();
        for (ItemOrder itemOrder : itemOrderList) {
            if (itemOrder.getName().toLowerCase().contains(query.toLowerCase()) || itemOrder.getBarcode().contains(query)) {
                filteredList.add(itemOrder);
            }
        }
        itemOrderAdapter.updateItems(filteredList);
    }

    private void updateTotalSelectedItems(int total) {
        totalSelectedItems = total;
        inCartCounter.setText(String.valueOf(totalSelectedItems));
    }

    private ArrayList<ItemOrder> getSelectedItems() {
        ArrayList<ItemOrder> selectedItems = new ArrayList<>();
        for (ItemOrder itemOrder : itemOrderList) {
            if (itemOrder.getSelectedQuantity() > 0) {
                selectedItems.add(itemOrder);
            }
        }
        return selectedItems;
    }

    private void resetInCartCounter() {
        totalSelectedItems = 0;
        inCartCounter.setText(String.valueOf(totalSelectedItems));
    }
}
