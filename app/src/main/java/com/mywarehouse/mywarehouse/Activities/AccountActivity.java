package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Adapters.RoleSpinnerAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseAccount;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;

import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private EditText emailInput, birthdayInput, passwordInput, confirmPasswordInput, phoneInput;
    private Spinner roleSpinner, countryCodeSpinner;
    private BottomNavigationView bottomNavigationView;
    private Intent intent = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean shouldCancelFetch = false;
    private String[] roles={"Picker","Warehouse worker","Warehouse worker and picker","Admin"};
    private int[] icons = {R.drawable.ic_picker,R.drawable.ic_warehouse_worker,
            R.drawable.ic_worker_picker,R.drawable.ic_admin}; // Use your icon resource i

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        findViews();

        // Setup the bottom navigation view based on the user role
        NavigationBarManager.getInstance().setupBottomNavigationView(bottomNavigationView, this);
        NavigationBarManager.getInstance().setNavigation(bottomNavigationView,this,R.id.navigation_account);
        // Update button listener
        findViewById(R.id.update_button).setOnClickListener(v -> {
            if (validateFields()) {
                updateUserData();
            }
        });

        fetchUserData();
    }

    private void findViews() {
        emailInput = findViewById(R.id.email_input);
        birthdayInput = findViewById(R.id.birthday_input);
        phoneInput = findViewById(R.id.phone_input);
        countryCodeSpinner = findViewById(R.id.country_code_spinner);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        roleSpinner = findViewById(R.id.role_spinner);
        bottomNavigationView = findViewById(R.id.bottom_navigation);


        RoleSpinnerAdapter roleAdapter = new RoleSpinnerAdapter(this, roles, icons);
        roleSpinner.setAdapter(roleAdapter);
    }

    private void fetchUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseAccount.fetchUserData(db, userId, new FirebaseAccount.FetchCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (shouldCancelFetch) return; // Exit if fetch operation should be canceled
                if (document != null && document.exists()) {
                    emailInput.setText(document.getString("email"));
                    birthdayInput.setText(document.getString("birthday"));
                    String phoneNumber = document.getString("phone");
                    if (phoneNumber != null && phoneNumber.contains("0")) {
                        String[] parts = phoneNumber.split("0", 2);
                        if (parts.length == 2) {
                            String countryCode = parts[0];
                            String phone = "0" + parts[1];
                            phoneInput.setText(phone);
                            String[] countryCodes = getResources().getStringArray(R.array.country_codes);
                            for (int i = 0; i < countryCodes.length; i++) {
                                if (countryCodes[i].equals(countryCode)) {
                                    countryCodeSpinner.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                    // Set the role in the spinner
                    String role = document.getString("role");
                    for (int i = 0; i < roles.length; i++) {
                        if (roles[i].equals(role)) {
                            roleSpinner.setSelection(i);
                            break;
                        }
                    }
                } else {
                    Toast.makeText(AccountActivity.this, "No such document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AccountActivity.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        String phone = phoneInput.getText().toString().trim();
        String countryCode = countryCodeSpinner.getSelectedItem().toString();
        String password = passwordInput.getText().toString().trim();

        if (!validatePhoneNumber(phone) || !validatePassword(password, confirmPasswordInput.getText().toString().trim())) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", countryCode + phone);
        updates.put("countryCode", countryCode);

        FirebaseAccount.updateUserData(db, mAuth, userId, updates, password, new FirebaseAccount.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AccountActivity.this, "User data updated successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AccountActivity.this, "Failed to update user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateFields() {
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validatePhoneNumber(phone)) {
            return false;
        }

        if (!validatePassword(password, confirmPassword)) {
            return false;
        }

        return true;
    }

    private boolean validatePhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return false;
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            phoneInput.setError("Please enter a valid phone number");
            phoneInput.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters long");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            passwordInput.setError("Password must contain at least one uppercase letter");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            passwordInput.setError("Password must contain at least one lowercase letter");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            passwordInput.setError("Password must contain at least one digit");
            passwordInput.requestFocus();
            return false;
        }

        if (!password.matches(".*[@#\\$%^&+=!].*")) {
            passwordInput.setError("Password must contain at least one special character (@#\\$%^&+=!)");
            passwordInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Confirm Password is required");
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }

        return true;
    }
}
