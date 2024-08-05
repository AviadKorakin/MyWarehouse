package com.mywarehouse.mywarehouse.Activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.mywarehouse.mywarehouse.Adapters.RoleSpinnerAdapter;
import com.mywarehouse.mywarehouse.Firebase.FirebaseRegister;
import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.R;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, birthdayInput, phoneInput, passwordInput, confirmPasswordInput;
    private Spinner roleSpinner, countryCodeSpinner;
    private Button registerButton, loginButton;

    private FirebaseAuth mAuth;

    private String[] roles = {"Picker", "Warehouse worker", "Warehouse worker and picker", "Admin"};
    private int[] icons = {R.drawable.ic_picker, R.drawable.ic_warehouse_worker, R.drawable.ic_worker_picker, R.drawable.ic_admin}; // Use your icon resource ids

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        // Find views
        findViews();

        RoleSpinnerAdapter roleAdapter = new RoleSpinnerAdapter(this, roles, icons);
        roleSpinner.setAdapter(roleAdapter);

        // Set up date picker for birthday input
        birthdayInput.setOnClickListener(v -> showDatePicker());

        // Set up login button to navigate to LoginActivity
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Set up register button to validate fields and register user
        registerButton.setOnClickListener(v -> {
            if (validateFields()) {
                registerUser();
            }
        });
    }

    private void findViews() {
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        birthdayInput = findViewById(R.id.birthday_input);
        phoneInput = findViewById(R.id.phone_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        roleSpinner = findViewById(R.id.role_spinner);
        countryCodeSpinner = findViewById(R.id.country_code_spinner);
        registerButton = findViewById(R.id.register_button);
        loginButton = findViewById(R.id.login_button);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                RegisterActivity.this,
                (view, year1, monthOfYear, dayOfMonth) -> birthdayInput.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private boolean validateFields() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String birthday = birthdayInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            return false;
        }

        if (TextUtils.isEmpty(birthday)) {
            birthdayInput.setError("Birthday is required");
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            return false;
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            phoneInput.setError("Please enter a valid phone number");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }

        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters long");
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            passwordInput.setError("Password must contain at least one uppercase letter");
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            passwordInput.setError("Password must contain at least one lowercase letter");
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            passwordInput.setError("Password must contain at least one digit");
            return false;
        }

        if (!password.matches(".*[@#\\$%^&+=!].*")) {
            passwordInput.setError("Password must contain at least one special character (@#\\$%^&+=!)");
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Confirm Password is required");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        FirebaseRegister.createUserWithEmailAndPassword(email, password, mAuth, new FirebaseRegister.FirestoreCallback() {
            @Override
            public void onSuccess() {
                saveUserToFirestore();
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(RegisterActivity.this, "User already exists, please log in.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUserToFirestore() {
        String userId = mAuth.getCurrentUser().getUid();
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String birthday = birthdayInput.getText().toString().trim();
        String phone = countryCodeSpinner.getSelectedItem().toString() + phoneInput.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString().trim();

        User user = new User(userId, name, email, birthday, phone, role, null, null);
        FirebaseRegister.saveUserToFirestore(user, new FirebaseRegister.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RegisterActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
