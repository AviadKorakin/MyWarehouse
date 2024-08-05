package com.mywarehouse.mywarehouse.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.mywarehouse.mywarehouse.Firebase.FirebaseLogin;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Utilities.NavigationBarManager;
import com.mywarehouse.mywarehouse.Utilities.MyUser;
import com.mywarehouse.mywarehouse.Models.User;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton, registerButton;
    private TextView notRegisteredText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        overridePendingTransition(R.anim.dark_screen, R.anim.light_screen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find views
        findViews();

        // Set up register button to navigate to RegisterActivity
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Set up login button to validate fields and log in the user
        loginButton.setOnClickListener(v -> {
            if (validateFields()) {
                loginUser();
            }
        });
    }

    private void findViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        notRegisteredText = findViewById(R.id.not_registered_text);
    }

    private boolean validateFields() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
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

        return true;
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        FirebaseLogin.signInWithEmailAndPassword(email, password, mAuth, new FirebaseLogin.FirestoreCallback() {
            @Override
            public void onSuccess() {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    fetchUserRole(user.getUid());
                }
            }

            @Override
            public void onFailure(Exception e) {
                String errorMessage;
                try {
                    throw e;
                } catch (FirebaseAuthInvalidUserException ex) {
                    errorMessage = "No account found with this email.";
                } catch (FirebaseAuthInvalidCredentialsException ex) {
                    errorMessage = "Incorrect password. Please try again.";
                } catch (Exception ex) {
                    errorMessage = "Authentication failed: " + ex.getMessage();
                }
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserRole(String userId) {
        FirebaseLogin.fetchUserRole(userId, user -> {
            if (user != null) {
                String role = user.getRole();
                String name = user.getName();
                if (role != null && name != null) {
                    MyUser.getInstance().setName(name);
                    MyUser.getInstance().setDocumentId(userId);
                    Toast.makeText(LoginActivity.this, "Welcome " + name + ".", Toast.LENGTH_SHORT).show();
                    navigateToRoleSpecificActivity(role);
                } else {
                    Toast.makeText(LoginActivity.this, "Role or name not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this, "User name or password is wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToRoleSpecificActivity(String role) {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        NavigationBarManager.getInstance(role);
        startActivity(intent);
        finish();
    }
}
