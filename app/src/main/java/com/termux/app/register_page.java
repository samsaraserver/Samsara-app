package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import java.util.concurrent.CompletableFuture;

public class register_page extends Activity {
    private static final String TAG = "RegisterPage";
    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailPhoneBox;
    private EditText passwordBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        try {
            SupabaseConfig.initialize();
            userRepository = new UserRepository();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Supabase: " + e.getMessage(), e);
            Toast.makeText(this, "Database connection error. Please try again later.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        TextView tvSignUp2 = findViewById(R.id.tvSignUp2);
        tvSignUp2.setText(Html.fromHtml(tvSignUp2.getText().toString()));

        usernameBox = findViewById(R.id.UsernameBox);
        emailPhoneBox = findViewById(R.id.EmailPhoneBox);
        passwordBox = findViewById(R.id.PasswordBox);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.SignInBtn);
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(register_page.this, login_page.class);
            startActivity(intent);
            finish();
        });

        ImageButton createAccountButton = findViewById(R.id.CreateAccountBtn);
        createAccountButton.setOnClickListener(view -> {
            handleCreateAccount();
        });

        ImageButton githubButton = findViewById(R.id.SignInGithubBtn);
        if (githubButton != null) {
            githubButton.setOnClickListener(view -> {
                Toast.makeText(this, "GitHub signup not implemented yet", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void handleCreateAccount() {
        String username = usernameBox.getText().toString().trim();
        String email = emailPhoneBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateInput(username, email, password)) {
            return;
        }

        setFormEnabled(false);
        Toast.makeText(this, "Checking availability...", Toast.LENGTH_SHORT).show();

        // First check if username or email already exists
        userRepository.checkUsernameExists(username)
            .thenCompose(usernameExists -> {
                if (usernameExists) {
                    runOnUiThread(() -> {
                        setFormEnabled(true);
                        usernameBox.setError("Username already exists");
                        usernameBox.requestFocus();
                    });
                    return CompletableFuture.completedFuture(false);
                }
                return userRepository.checkEmailExists(email);
            })
            .thenCompose(emailExists -> {
                if (emailExists) {
                    runOnUiThread(() -> {
                        setFormEnabled(true);
                        emailPhoneBox.setError("Email already registered");
                        emailPhoneBox.requestFocus();
                    });
                    return CompletableFuture.completedFuture(false);
                }
                
                runOnUiThread(() -> Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show());
                return userRepository.createUser(username, email, password);
            })
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    if (success) {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show();
                        
                        Intent intent = new Intent(register_page.this, home_page.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to create account. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error creating account", throwable);
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(this, "Error creating account: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }

    private boolean validateInput(String username, String email, String password) {
        if (TextUtils.isEmpty(username)) {
            usernameBox.setError("Username is required");
            usernameBox.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            usernameBox.setError("Username must be at least 3 characters");
            usernameBox.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailPhoneBox.setError("Email is required");
            emailPhoneBox.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailPhoneBox.setError("Please enter a valid email address");
            emailPhoneBox.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordBox.setError("Password is required");
            passwordBox.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            passwordBox.setError("Password must be at least 8 characters");
            passwordBox.requestFocus();
            return false;
        }

        return true;
    }

    private void setFormEnabled(boolean enabled) {
        usernameBox.setEnabled(enabled);
        emailPhoneBox.setEnabled(enabled);
        passwordBox.setEnabled(enabled);
        findViewById(R.id.CreateAccountBtn).setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRepository != null) {
            userRepository.shutdown();
        }
    }
}
