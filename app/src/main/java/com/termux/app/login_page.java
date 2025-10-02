package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import java.util.concurrent.CompletableFuture;

public class login_page extends Activity {
    private static final String TAG = "LoginPage";
    private UserRepository userRepository;
    private EditText emailUsernameBox;
    private EditText passwordBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        try {
            SupabaseConfig.initialize(this);
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
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setText(Html.fromHtml(tvForgotPassword.getText().toString()));

        TextView tvSignUp2 = findViewById(R.id.tvSignUp2);
        tvSignUp2.setText(Html.fromHtml(tvSignUp2.getText().toString()));

        emailUsernameBox = findViewById(R.id.UsernameBox);
        passwordBox = findViewById(R.id.PasswordBox);
    }

    private void setupClickListeners() {
        ImageButton signUpButton = findViewById(R.id.SignUpBtn);
        Intent registerIntent = new Intent(login_page.this, register_page.class);
        signUpButton.setOnClickListener(view -> {
            startActivity(registerIntent);
            finish();
        });

        ImageButton loginButton = findViewById(R.id.SignInBtn);
        if (loginButton != null) {
            loginButton.setOnClickListener(view -> handleLogin());
        }

        ImageButton continueWithoutAccountButton = findViewById(R.id.ContinueWithoutAccountBtn);
        Intent homeIntent = new Intent(login_page.this, home_page.class);
        continueWithoutAccountButton.setOnClickListener(view -> {
            startActivity(homeIntent);
            finish();
        });

        ImageButton githubButton = findViewById(R.id.LoginGithubGtn);
        if (githubButton != null) {
            githubButton.setOnClickListener(view -> {
                Toast.makeText(this, "GitHub login not implemented yet", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void handleLogin() {
        String emailOrUsername = emailUsernameBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateInput(emailOrUsername, password)) {
            return;
        }

        setFormEnabled(false);
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();

        userRepository.authenticateUser(emailOrUsername, password)
            .thenCompose(success -> {
                if (success) {
                    Log.d(TAG, "Authentication successful, fetching user data...");
                    if (emailOrUsername.contains("@")) {
                        return userRepository.getUserByEmail(emailOrUsername);
                    } else {
                        return userRepository.getUserByUsername(emailOrUsername);
                    }
                } else {
                    Log.d(TAG, "Authentication failed");
                    CompletableFuture<com.termux.app.database.models.SamsaraUser> future = new CompletableFuture<>();
                    future.complete(null);
                    return future;
                }
            })
            .thenAccept(user -> {
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    if (user != null) {
                        Log.d(TAG, "User data retrieved successfully: " + user.getUsername());
                        AuthManager.getInstance(this).loginUser(user);
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(login_page.this, home_page.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Invalid email/username or password", Toast.LENGTH_LONG).show();
                        passwordBox.setText("");
                        passwordBox.requestFocus();
                    }
                });
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error during login", throwable);
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(this, "Login error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }

    private boolean validateInput(String emailOrUsername, String password) {
        if (TextUtils.isEmpty(emailOrUsername)) {
            emailUsernameBox.setError("Email or username is required");
            emailUsernameBox.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordBox.setError("Password is required");
            passwordBox.requestFocus();
            return false;
        }

        return true;
    }

    private void setFormEnabled(boolean enabled) {
        emailUsernameBox.setEnabled(enabled);
        passwordBox.setEnabled(enabled);
        findViewById(R.id.SignInBtn).setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRepository != null) {
            userRepository.shutdown();
        }
    }
}
