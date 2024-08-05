package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Adapters.CheckoutItemOrderAdapter;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseCheckout;
import com.mywarehouse.mywarehouse.Models.ItemOrder;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCheckoutItems;
    private CheckoutItemOrderAdapter checkoutItemOrderAdapter;
    private List<ItemOrder> itemOrderList;
    private MaterialButton buttonOrder, buttonCancel;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        db = FirebaseFirestore.getInstance();

        recyclerViewCheckoutItems = findViewById(R.id.recycler_view_checkout_items);
        buttonOrder = findViewById(R.id.button_order);
        buttonCancel = findViewById(R.id.button_cancel);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerViewCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        itemOrderList = getIntent().getParcelableArrayListExtra("selectedItems");
        checkoutItemOrderAdapter = new CheckoutItemOrderAdapter(this, itemOrderList, this::updateTotalSelectedItems);
        recyclerViewCheckoutItems.setAdapter(checkoutItemOrderAdapter);

        buttonCancel.setOnClickListener(v -> {
            Intent intent = new Intent(CheckoutActivity.this, AddOrderActivity.class);
            startActivity(intent);
            finish();
        });

        buttonOrder.setOnClickListener(v -> processOrder());

        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_orders);

        bottomNavigationView.setSelectedItemId(R.id.navigation_orders);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                intent = new Intent(CheckoutActivity.this, HomeActivity.class);
            } else if (id == R.id.navigation_inventory) {
                intent = new Intent(CheckoutActivity.this, InventoryActivity.class);
            } else if (id == R.id.navigation_reports) {
                intent = new Intent(CheckoutActivity.this, ReportsActivity.class);
            } else if (id == R.id.navigation_orders) {
                intent = new Intent(CheckoutActivity.this, OrdersActivity.class);
                return true;
            } else if (id == R.id.navigation_account) {
                intent = new Intent(CheckoutActivity.this, AccountActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            }

            return true;
        });
    }

    private void processOrder() {
        if (itemOrderList.isEmpty()) {
            Toast.makeText(this, "No items to order", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseCheckout.fetchItems(db, itemOrderList, new FirebaseCheckout.ItemsCallback() {
            @Override
            public void onItemsFetched(List<ItemOrder> items, boolean canOrder, List<DocumentReference> itemsToUpdate) {
                if (canOrder) {
                    String createdBy = MyUser.getInstance().getName();
                    FirebaseCheckout.placeOrder(db, itemsToUpdate, items, createdBy, new FirebaseCheckout.CheckoutCallback() {
                        @Override
                        public void onSuccess(String orderId) {
                            FirebaseCheckout.saveOrderForUser(db, MyUser.getInstance().getDocumentId(), orderId, new FirebaseCheckout.CheckoutCallback() {
                                @Override
                                public void onSuccess(String orderId) {
                                    Toast.makeText(CheckoutActivity.this, "Order placed successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(CheckoutActivity.this, OrdersActivity.class);
                                    startActivity(intent);
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(CheckoutActivity.this, "Failed to update user orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(CheckoutActivity.this, "Error placing order", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    for (ItemOrder itemOrder : items) {
                        checkoutItemOrderAdapter.setMaxQuantityForItem(itemOrder);
                        Toast.makeText(CheckoutActivity.this, "Not enough quantity for item: " + itemOrder.getName(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CheckoutActivity.this, "Error fetching items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotalSelectedItems(int total) {
        // You can update UI or any other component with the total selected items if needed.
    }
}
