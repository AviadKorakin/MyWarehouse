package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mywarehouse.mywarehouse.Adapters.TransactionRequestAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Firebase.FirebaseRequests;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.List;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRequests;
    private TransactionRequestAdapter transactionRequestAdapter;
    private List<TransactionRequest> transactionRequestList;
    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<Intent> acceptTransactionActivityResultLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        recyclerViewRequests = findViewById(R.id.recycler_view_requests);
        transactionRequestList = new ArrayList<>();

        // Initialize the adapter before setting it to the RecyclerView
        transactionRequestAdapter = new TransactionRequestAdapter(this, transactionRequestList);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRequests.setAdapter(transactionRequestAdapter);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationBar();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(RequestsActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
        // Set up the real-time listener for transaction requests
        setupRealtimeTransactionRequestListener();

        acceptTransactionActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(RequestsActivity.this, "Transaction accepted successfully", Toast.LENGTH_SHORT).show();
                    }
                    // No need to manually fetch data here since real-time listener is active
                }
        );
    }

    private void setupRealtimeTransactionRequestListener() {
        FirebaseRequests.listenToTransactionRequests(new FirebaseRequests.FetchCallback<TransactionRequest>() {
            @Override
            public void onSuccess(List<TransactionRequest> requests) {
                transactionRequestList.clear();
                transactionRequestList.addAll(requests);
                transactionRequestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RequestsActivity.this, "Error getting transaction requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_inventory);
    }

    public void startAcceptTransactionActivity(TransactionRequest request) {
        Intent intent = new Intent(this, AcceptTransactionActivity.class);
        intent.putExtra("transactionRequest", request);
        acceptTransactionActivityResultLauncher.launch(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseForAdapters.removeAllItemChangeListeners();
    }
}
