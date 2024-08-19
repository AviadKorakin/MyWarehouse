package com.mywarehouse.mywarehouse.Activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

public class ReportsActivity extends AppCompatActivity {

    private static final String TAG = "ReportsActivity";

    private RecyclerView recyclerViewLogs;
    private LogAdapter logAdapter;
    private List<MyLog> logList = new ArrayList<>();
    private EditText dateInput;
    private Spinner typeSpinner;
    private BottomNavigationView bottomNavigationView;
    private Date selectedDate;
    private View progressBar;
    private int totalLogCount = 0;
    private int loadedLogCount = 0;
    private boolean isInitialLoadComplete = false;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        initViews();
        setupNavigationBar();
        setupDateInput();
        setupTypeSpinner();
        showLoading();
        fetchTotalLogCount();
    }

    private void initViews() {
        recyclerViewLogs = findViewById(R.id.recycler_logs);
        dateInput = findViewById(R.id.date_input);
        typeSpinner = findViewById(R.id.type_spinner);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.progress_bar);
        linearLayout = findViewById(R.id.search_container);
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(logList);
        recyclerViewLogs.setAdapter(logAdapter);

    }

    private void setupNavigationBar() {
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView, this, R.id.navigation_reports);
    }

    private void setupDateInput() {
        dateInput.setOnClickListener(v -> showDatePicker());
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
                    filterLogsByDateAndType();
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void setupTypeSpinner() {
        ArrayAdapter<LogType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LogType.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLogsByDateAndType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void fetchTotalLogCount() {
        FirebaseLogs.fetchTotalLogCount(new FirebaseLogs.LogCountCallback() {
            @Override
            public void onLogCountFetched(int count) {
                totalLogCount = count;
                if(totalLogCount==0)
                {
                    Toast.makeText(ReportsActivity.this, "No logs to show", Toast.LENGTH_SHORT).show();
                    hideLoading(); // Hide loading on failure
                }
                else {
                    setupRealtimeLogListener();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch log count", e);
                Toast.makeText(ReportsActivity.this, "Failed to fetch log count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void setupRealtimeLogListener() {
        FirebaseLogs.listenToLogs(new FirebaseLogs.LogsCallback() {
            @Override
            public void onLogAdded(MyLog log) {
                logList.add(log);
                loadedLogCount++;
                filterLogsByDateAndType();
                checkInitialLoadComplete();
            }

            @Override
            public void onLogModified(MyLog log) {
                int index = findLogIndexById(log.getId());
                if (index != -1) {
                    logList.set(index, log);
                    filterLogsByDateAndType();
                }
            }

            @Override
            public void onLogRemoved(String logId) {
                int index = findLogIndexById(logId);
                if (index != -1) {
                    logList.remove(index);
                    loadedLogCount--;
                    filterLogsByDateAndType();
                    checkInitialLoadComplete();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to listen to logs", e);
                Toast.makeText(ReportsActivity.this, "Failed to listen to logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                hideLoading(); // Hide loading on failure
            }
        });
    }

    private void filterLogsByDateAndType() {
        LogType selectedType = (LogType) typeSpinner.getSelectedItem();
        List<MyLog> filteredLogs = new ArrayList<>();

        for (MyLog log : logList) {
            boolean matchesDate = selectedDate == null || new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(log.getDate()).equals(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate));
            boolean matchesType = selectedType == LogType.ALL || log.getType() == selectedType;

            if (matchesDate && matchesType) {
                filteredLogs.add(log);
            }
        }

        logAdapter.setLogList(filteredLogs);
    }

    private int findLogIndexById(String logId) {
        for (int i = 0; i < logList.size(); i++) {
            if (logList.get(i).getId().equals(logId)) {
                return i;
            }
        }
        return -1;
    }

    private void checkInitialLoadComplete() {
        if (loadedLogCount >= totalLogCount && !isInitialLoadComplete) {
            isInitialLoadComplete = true;
            hideLoading();
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewLogs.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerViewLogs.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseLogs.removeAllListeners(); // Remove listeners when the activity is destroyed
    }
}
