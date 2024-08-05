package com.mywarehouse.mywarehouse.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Adapters.LogAdapter;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseLogs;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {

    private static final String TAG = "ReportsActivity";

    private RecyclerView recyclerViewLogs;
    private LogAdapter logAdapter;
    private List<MyLog> logList = new ArrayList<>();
    private FirebaseFirestore db;
    private EditText dateInput;
    private Spinner typeSpinner;
    private BottomNavigationView bottomNavigationView;
    private ExecutorService executorService;
    private Intent intent;
    private Date selectedDate;

    private final int REFRESH_INTERVAL = 10000; // 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();

        recyclerViewLogs = findViewById(R.id.recycler_logs);
        dateInput = findViewById(R.id.date_input);
        typeSpinner = findViewById(R.id.type_spinner);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(logList);
        recyclerViewLogs.setAdapter(logAdapter);

        // Initialize Navigation Bar
        setupNavigationBar();
        // Setup date picker
        dateInput.setOnClickListener(v -> showDatePicker());

        // Setup type spinner
        setupTypeSpinner();

        initialLoadLogs();
        AppCompatImageButton refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> fetchLogs());

        // Fetch logs every 10 seconds
        executorService.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    fetchLogs();
                    Thread.sleep(REFRESH_INTERVAL);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "ExecutorService interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ReportsActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    dateInput.setText(sdf.format(selectedDate.getTime()));
                    this.selectedDate = selectedDate.getTime();
                    filterLogsByDateAndType(this.selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void initialLoadLogs() {
        FirebaseLogs.fetchLogs(db, new FirebaseLogs.LogsCallback() {
            @Override
            public void onCallback(List<MyLog> logs) {
                logList.clear();
                logList.addAll(logs);
                filterLogsByType(); // Filter logs by selected type
                logAdapter.setLogList(new ArrayList<>(logList));
                Log.d(TAG, "Initial logs loaded: " + logList.size());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load initial logs", e);
                Toast.makeText(ReportsActivity.this, "Failed to load logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLogs() {
        FirebaseLogs.fetchLogs(db, new FirebaseLogs.LogsCallback() {
            @Override
            public void onCallback(List<MyLog> logs) {
                runOnUiThread(() -> {
                    logList.clear();
                    logList.addAll(logs);
                    filterLogsByDateAndType(selectedDate); // Apply date and type filters after fetching logs
                });
                Log.d(TAG, "Logs fetched: " + logs.size());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch logs", e);
                runOnUiThread(() -> Toast.makeText(ReportsActivity.this, "Failed to fetch logs: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterLogsByDateAndType(Date date) {
        if (date == null) {
            filterLogsByType(); // If no date is selected, filter only by type
            return;
        }

        LogType selectedType = (LogType) typeSpinner.getSelectedItem();
        FirebaseLogs.filterLogsByDateAndType(db, date, selectedType, new FirebaseLogs.LogsCallback() {
            @Override
            public void onCallback(List<MyLog> logs) {
                logList.clear();
                logList.addAll(logs);
                logAdapter.setLogList(new ArrayList<>(logList));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to filter logs", e);
                Toast.makeText(ReportsActivity.this, "Failed to filter logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterLogsByType() {
        LogType selectedType = (LogType) typeSpinner.getSelectedItem();
        if (selectedType == LogType.ALL) {
            logAdapter.setLogList(new ArrayList<>(logList)); // Show all logs
            return;
        }
        List<MyLog> filteredLogs = new ArrayList<>();
        for (MyLog log : logList) {
            if (log.getType() == selectedType) {
                filteredLogs.add(log);
            }
        }
        logAdapter.setLogList(new ArrayList<>(filteredLogs));
    }

    private void setupTypeSpinner() {
        ArrayAdapter<LogType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LogType.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLogsByType();
                if (selectedDate != null) {
                    filterLogsByDateAndType(selectedDate); // Apply date filter if a date is selected
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_reports);
    }
}
