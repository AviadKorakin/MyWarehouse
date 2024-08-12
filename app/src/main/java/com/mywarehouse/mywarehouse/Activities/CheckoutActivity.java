package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;

import com.mywarehouse.mywarehouse.Adapters.CheckoutItemOrderAdapter;

import com.mywarehouse.mywarehouse.Firebase.FirebaseCheckout;
import com.mywarehouse.mywarehouse.Models.ItemOrder;

import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.MyUser;



import java.util.List;


public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCheckoutItems;
    private CheckoutItemOrderAdapter checkoutItemOrderAdapter;
    private List<ItemOrder> itemOrderList;
    private MaterialButton buttonOrder, buttonCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        recyclerViewCheckoutItems = findViewById(R.id.recycler_view_checkout_items);
        buttonOrder = findViewById(R.id.button_order);
        buttonCancel = findViewById(R.id.button_cancel);

        recyclerViewCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        itemOrderList = getIntent().getParcelableArrayListExtra("selectedItems");
        checkoutItemOrderAdapter = new CheckoutItemOrderAdapter(this, itemOrderList, this::updateTotalSelectedItems);
        recyclerViewCheckoutItems.setAdapter(checkoutItemOrderAdapter);

        buttonCancel.setOnClickListener(v -> {
            finish();
        });

        buttonOrder.setOnClickListener(v -> processOrder());

    }

    private void processOrder() {
        if (itemOrderList.isEmpty()) {
            Toast.makeText(this, "No items to order", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseCheckout.fetchItems(itemOrderList, new FirebaseCheckout.ItemsCallback() {
            @Override
            public void onItemsFetched(List<ItemOrder> items, boolean canOrder, List<DocumentReference> itemsToUpdate) {
                if (canOrder) {
                    String createdBy = MyUser.getInstance().getUser().getName();
                    FirebaseCheckout.placeOrder(itemsToUpdate, items, createdBy, new FirebaseCheckout.CheckoutCallback() {
                        @Override
                        public void onSuccess(String orderId) {
                            FirebaseCheckout.saveOrderForUser(MyUser.getInstance().getUser().getUserId(), orderId, new FirebaseCheckout.CheckoutCallback() {
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
