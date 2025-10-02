package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;

public class login_page extends Activity {
    private static final String TAG = "LoginPage";
    private static final String PREFS_NAME = "SamsaraLoginPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL_USERNAME = "email_username";
    private static final String KEY_PASSWORD = "password";

    private UserRepository userRepository;
    private EditText emailUsernameBox;
    private EditText passwordBox;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        loginPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
        loadSavedCredentials();
        setupClickListeners();
    }

    private void initializeViews() {
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setText(Html.fromHtml(tvForgotPassword.getText().toString()));
        }

        TextView tvRegister2 = findViewById(R.id.tvRegister2);
        if (tvRegister2 != null) {
            tvRegister2.setText(Html.fromHtml(tvRegister2.getText().toString()));
        }

        emailUsernameBox = findViewById(R.id.EmailUsernameBox);
        passwordBox = findViewById(R.id.PasswordBox);
        rememberMeCheckBox = findViewById(R.id.RememberMeCheckBox);
    }

    private void loadSavedCredentials() {
        boolean rememberMe = loginPrefs.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedUsername = loginPrefs.getString(KEY_EMAIL_USERNAME, "");
            String savedPassword = loginPrefs.getString(KEY_PASSWORD, "");

            // Pre-fill the form with saved credentials
            emailUsernameBox.setText(savedUsername);
            passwordBox.setText(savedPassword);
            rememberMeCheckBox.setChecked(true);

            // No automatic login - user must click the login button themselves
            Toast.makeText(this, "Credentials loaded. Click Login to continue.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCredentials(String emailOrUsername, String password) {
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.putString(KEY_EMAIL_USERNAME, emailOrUsername);
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_EMAIL_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.apply();
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
                    java.util.concurrent.CompletableFuture<com.termux.app.database.models.SamsaraUser> future = new java.util.concurrent.CompletableFuture<>();
                    future.complete(null);
                    return future;
                }
            })
            .thenAccept(user -> {
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    if (user != null) {
                        Log.d(TAG, "User data retrieved successfully: " + user.getUsername());

                        // Handle Remember Me functionality
                        if (rememberMeCheckBox.isChecked()) {
                            saveCredentials(emailOrUsername, password);
                        } else {
                            clearSavedCredentials();
                        }

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
