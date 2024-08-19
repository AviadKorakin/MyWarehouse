package com.mywarehouse.mywarehouse.Activities;

import android.annotation.SuppressLint;
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
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.mywarehouse.mywarehouse.Adapters.ItemOrderAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseAddOrder;
import com.mywarehouse.mywarehouse.Models.ItemOrder;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddOrderActivity extends AppCompatActivity {

    private TextInputEditText searchInput;
    private AppCompatTextView inCartCounter;
    private RecyclerView recyclerViewItems;
    private BottomNavigationView bottomNavigationView;
    private ItemOrderAdapter itemOrderAdapter;
    private List<ItemOrder> itemOrderList;
    private AppCompatImageButton buttonScanBarcode, buttonCart;
    private ProgressBar progressBar;
    private Map<String, ItemOrder> itemMap;
    private int totalSelectedItems = 0;
    private int totalItemCount = -1;
    private int loadedItemCount = 0;
    private boolean isInitialLoadComplete = false;
    private ConstraintLayout constraintLayout;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            searchInput.setText(result.getContents());
        }
    });

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = new Intent(AddOrderActivity.this, OrdersActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AddOrderActivity.this, "Checkout canceled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        showLoading();  // Show loading indicator
        setupRealtimeListener();  // Set up the real-time listener
    }

    private void initViews() {
        searchInput = findViewById(R.id.search_input);
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonScanBarcode = findViewById(R.id.button_scan_barcode);
        buttonCart = findViewById(R.id.cart_button);
        inCartCounter = findViewById(R.id.incart_number);
        progressBar = findViewById(R.id.progress_bar);
        constraintLayout = findViewById(R.id.search_container);

        itemOrderList = new ArrayList<>();
        itemMap = new HashMap<>();
        itemOrderAdapter = new ItemOrderAdapter(this, itemOrderList, this::updateTotalSelectedItems);
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(itemOrderAdapter);

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
                checkoutLauncher.launch(intent);
            } else {
                Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(AddOrderActivity.this, OrdersActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupRealtimeListener() {
        FirebaseAddOrder.listenToItems(new FirebaseAddOrder.ItemsCallback() {
            @Override
            public void onItemAdded(ItemOrder item) {
                itemMap.put(item.getBarcode() + "_" + item.getName(), item);
                itemOrderAdapter.addItem(item);
                loadedItemCount++;
                checkInitialLoadComplete();
            }

            @Override
            public void onItemModified(ItemOrder item) {
                itemMap.put(item.getBarcode() + "_" + item.getName(), item);
                itemOrderAdapter.updateItem(item);
            }

            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onItemRemoved(String itemId) {
                itemMap.remove(itemId);
                int toDiscount= findItemIndexById(itemId);
                if(toDiscount!=0 && toDiscount!=-1)
                updateTotalSelectedItems(totalSelectedItems-toDiscount);
                itemOrderAdapter.removeItem(itemId);
                loadedItemCount--;
                checkInitialLoadComplete();
            }

            @Override
            public void onItemCountFetched(int count) {
                totalItemCount = count;
                checkInitialLoadComplete();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AddOrderActivity.this, "Error fetching items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
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
    private int findItemIndexById(String itemId) {
        for (int i = 0; i < itemOrderList.size(); i++) {
            String currentId = itemOrderList.get(i).getBarcode() + "_" + itemOrderList.get(i).getName();
            if (currentId.equals(itemId)) {
               return itemOrderList.get(i).getSelectedQuantity();
            }
        }
        return -1;
    }
    private void checkInitialLoadComplete() {
        if (totalItemCount != -1 && loadedItemCount >= totalItemCount && !isInitialLoadComplete) {
            isInitialLoadComplete = true;
            hideLoading();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewItems.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewItems.setVisibility(View.VISIBLE);
        constraintLayout.setVisibility(View.VISIBLE);
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_orders);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseAddOrder.removeAllListeners(); // Remove listeners when the activity is destroyed
    }
}
