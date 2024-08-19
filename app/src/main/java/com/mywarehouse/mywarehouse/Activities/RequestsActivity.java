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
import com.mywarehouse.mywarehouse.Adapters.TransactionRequestAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Firebase.FirebaseRequests;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRequests;
    private TransactionRequestAdapter transactionRequestAdapter;
    private List<TransactionRequest> transactionRequestList;
    private Map<String, TransactionRequest> transactionRequestMap;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> acceptTransactionActivityResultLauncher;
    private int totalRequestCount = 0;
    private int loadedRequestCount = 0;
    private boolean isInitialLoadComplete = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        showLoading();  // Show loading indicator
        fetchTotalRequestCount();  // Fetch the total number of transaction requests

        acceptTransactionActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(RequestsActivity.this, "Transaction accepted successfully", Toast.LENGTH_SHORT).show();
                    }
                    // No need to manually fetch data here since real-time listener is active
                }
        );

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(RequestsActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initViews() {
        recyclerViewRequests = findViewById(R.id.recycler_view_requests);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);

        transactionRequestList = new ArrayList<>();
        transactionRequestMap = new HashMap<>();
        transactionRequestAdapter = new TransactionRequestAdapter(this, transactionRequestList);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRequests.setAdapter(transactionRequestAdapter);
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        bottomNavigationView.setSelectedItemId(R.id.navigation_inventory);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_inventory);
    }

    private void fetchTotalRequestCount() {
        FirebaseRequests.fetchTotalRequestCount(new FirebaseRequests.RequestCountCallback() {
            @Override
            public void onRequestCountFetched(int count) {
                totalRequestCount = count;
                if(totalRequestCount==0)
                {
                    Toast.makeText(RequestsActivity.this, "No requests to show", Toast.LENGTH_SHORT).show();
                    hideLoading(); // Hide loading on failure
                }
                setupRealtimeTransactionRequestListener();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RequestsActivity.this, "Failed to fetch total request count", Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void setupRealtimeTransactionRequestListener() {
        FirebaseRequests.listenToTransactionRequests(new FirebaseRequests.TransactionRequestCallback() {
            @Override
            public void onRequestAdded(TransactionRequest request) {
                transactionRequestMap.put(request.getRequestId(), request);
                loadedRequestCount++;
                updateTransactionRequestList();
                checkInitialLoadComplete();
            }

            @Override
            public void onRequestModified(TransactionRequest request) {
                transactionRequestMap.put(request.getRequestId(), request);
                updateTransactionRequestList();
            }

            @Override
            public void onRequestRemoved(String requestId) {
                transactionRequestMap.remove(requestId);
                loadedRequestCount++;
                updateTransactionRequestList();
                checkInitialLoadComplete();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RequestsActivity.this, "Error getting transaction requests", Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void updateTransactionRequestList() {
        transactionRequestList.clear();
        transactionRequestList.addAll(transactionRequestMap.values());
        transactionRequestAdapter.notifyDataSetChanged();
    }

    private void checkInitialLoadComplete() {
        if (loadedRequestCount >= totalRequestCount && !isInitialLoadComplete) {
            isInitialLoadComplete = true;
            hideLoading(); // Hide the loading screen after the initial data load is complete
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewRequests.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewRequests.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseForAdapters.removeAllItemChangeListeners(); // Remove listeners when the activity is destroyed
        FirebaseRequests.removeAllListeners();
    }

    public void startAcceptTransactionActivity(TransactionRequest request) {
        Intent intent = new Intent(this, AcceptTransactionActivity.class);
        intent.putExtra("transactionRequest", request);
        acceptTransactionActivityResultLauncher.launch(intent);
    }
}
