package com.termux.app;

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
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricPrompt;
import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;

public class login_page extends FragmentActivity {
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
    private BiometricHelper biometricHelper;

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

        // Initialize biometric helper with callback - REMOVED RISKY AUTO-TRIGGER
        biometricHelper = new BiometricHelper(this, new BiometricHelper.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                // Handle successful authentication
                loginWithStoredCredentials(username, email, password);
            }

            @Override
            public void onAuthenticationFailed() {
                // Handle authentication failure
                Toast.makeText(login_page.this, "Biometric authentication failed. Please try again or use your password.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                // Handle authentication errors - most likely user cancelled
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(login_page.this, "Authentication error: " + errString, Toast.LENGTH_LONG).show();
                }
            }
        });
        setupBiometricLogin();
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

        // REMOVED RISKY FOCUS LISTENERS THAT COULD CAUSE CRASHES
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
        ImageButton RegisterButton = findViewById(R.id.RegisterBtn);
        Intent registerIntent = new Intent(login_page.this, register_page.class);
        RegisterButton.setOnClickListener(view -> {
            startActivity(registerIntent);
            finish();
        });

        ImageButton Register2Button = findViewById(R.id.RegisterBtn2);
        Intent registerIntent2 = new Intent(login_page.this, register_page.class);
        Register2Button.setOnClickListener(view -> {
            startActivity(registerIntent2);
            finish();
        });

        ImageButton BiometricLogin = findViewById(R.id.LoginBiometricsBtn); // Fixed ID mismatch
        BiometricLogin.setOnClickListener(view -> {
            if (biometricHelper != null) {
                if (biometricHelper.isBiometricAvailable() && biometricHelper.hasStoredCredentials()) {
                    Log.d(TAG, "Starting biometric authentication from biometric button");
                    biometricHelper.startBiometricAuthentication();
                } else {
                    Toast.makeText(this, "Biometric authentication not available or no stored credentials.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Biometric helper not initialized.", Toast.LENGTH_SHORT).show();
            }
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

    private void setupBiometricLogin() {
        if (biometricHelper == null) {
            Log.e(TAG, "BiometricHelper is null, cannot setup biometric login");
            return;
        }

        if (!biometricHelper.isBiometricAvailable()) {
            // Hide or disable biometric option if not available
            ImageButton biometricLoginButton = findViewById(R.id.LoginBiometricsBtn); // Fixed ID mismatch
            if (biometricLoginButton != null) {
                biometricLoginButton.setEnabled(false);
                biometricLoginButton.setAlpha(0.5f);
                Log.d(TAG, "Biometric authentication not available, button disabled");
            }
        } else {
            Log.d(TAG, "Biometric authentication is available");
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

        // Use simple AsyncTask-style approach for API 21+ compatibility
        new Thread(() -> {
            try {
                // First authenticate the user
                userRepository.authenticateUser(emailOrUsername, password)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            runOnUiThread(() -> {
                                setFormEnabled(true);
                                Log.e(TAG, "Authentication error", throwable);
                                Toast.makeText(this, "Login error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            });
                            return null;
                        }

                        if (success != null && success) {
                            Log.d(TAG, "Authentication successful, fetching user data...");
                            // Now fetch user data
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (emailOrUsername.contains("@")) {
                                        user = userRepository.getUserByEmail(emailOrUsername).get();
                                    } else {
                                        user = userRepository.getUserByUsername(emailOrUsername).get();
                                    }

                                    final SamsaraUser finalUser = user;
                                    runOnUiThread(() -> handleUserResult(finalUser, null, emailOrUsername, password));
                                } catch (Exception e) {
                                    runOnUiThread(() -> handleUserResult(null, e, emailOrUsername, password));
                                }
                            }).start();
                        } else {
                            Log.d(TAG, "Authentication failed");
                            runOnUiThread(() -> {
                                setFormEnabled(true);
                                Toast.makeText(this, "Invalid email/username or password", Toast.LENGTH_LONG).show();
                                passwordBox.setText("");
                                passwordBox.requestFocus();
                            });
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during login", e);
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleUserResult(SamsaraUser user, Throwable userThrowable, String emailOrUsername, String password) {
        runOnUiThread(() -> {
            setFormEnabled(true);
            if (userThrowable != null) {
                Log.e(TAG, "Error fetching user data", userThrowable);
                Toast.makeText(this, "Login error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (user != null) {
                Log.d(TAG, "User data retrieved successfully: " + user.getUsername());

                // Handle Remember Me functionality
                if (rememberMeCheckBox.isChecked()) {
                    saveCredentials(emailOrUsername, password);
                } else {
                    clearSavedCredentials();
                }

                // Store credentials securely for biometric authentication
                storeCredentialsForBiometric(user, emailOrUsername, password);

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
    }

    private void storeCredentialsForBiometric(SamsaraUser user, String emailOrUsername, String password) {
        if (user == null) {
            Log.e(TAG, "Cannot store biometric credentials: user is null");
            return;
        }

        if (biometricHelper == null) {
            Log.w(TAG, "BiometricHelper is null, cannot store credentials for biometric authentication");
            return;
        }

        // Always store/update biometric credentials if biometric authentication is available
        // This ensures credentials are fresh and the biometric button will work
        if (biometricHelper.isBiometricAvailable()) {
            String username = user.getUsername();
            String email = user.getEmail();
            String userId = String.valueOf(user.getId());

            Log.d(TAG, "Storing/updating credentials for biometric authentication - Username: " + username + ", Email: " + email);

            // Store credentials securely for biometric authentication
            biometricHelper.storeCredentials(username, email, password, userId);

            // Inform user that biometric login is now available
            Toast.makeText(this, "Biometric login updated and ready to use!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithStoredCredentials(String username, String email, String password) {
        setFormEnabled(false);
        Toast.makeText(this, "Logging in with biometrics...", Toast.LENGTH_SHORT).show();

        // Determine whether to use email or username for login
        final String loginIdentifier = !TextUtils.isEmpty(email) ? email : username;

        if (TextUtils.isEmpty(loginIdentifier) || TextUtils.isEmpty(password)) {
            Log.e(TAG, "Stored credentials are incomplete");
            Toast.makeText(this, "Stored login information is incomplete. Please log in with password.",
                Toast.LENGTH_LONG).show();
            setFormEnabled(true);
            return;
        }

        // Use simple AsyncTask-style approach for API 21+ compatibility
        new Thread(() -> {
            try {
                // First authenticate the user
                userRepository.authenticateUser(loginIdentifier, password)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            runOnUiThread(() -> {
                                setFormEnabled(true);
                                Log.e(TAG, "Error during biometric login", throwable);
                                Toast.makeText(this, "Biometric login error: " + throwable.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            });
                            return null;
                        }

                        if (success != null && success) {
                            Log.d(TAG, "Biometric login authentication successful, fetching user data...");
                            // Now fetch user data
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (loginIdentifier.contains("@")) {
                                        user = userRepository.getUserByEmail(loginIdentifier).get();
                                    } else {
                                        user = userRepository.getUserByUsername(loginIdentifier).get();
                                    }

                                    final SamsaraUser finalUser = user;
                                    runOnUiThread(() -> handleBiometricUserResult(finalUser, null));
                                } catch (Exception e) {
                                    runOnUiThread(() -> handleBiometricUserResult(null, e));
                                }
                            }).start();
                        } else {
                            Log.d(TAG, "Biometric login authentication failed");
                            runOnUiThread(() -> {
                                setFormEnabled(true);
                                Toast.makeText(this, "Biometric login failed. Your stored credentials may no longer be valid.",
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during biometric login", e);
                runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(this, "Biometric login error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleBiometricUserResult(SamsaraUser user, Throwable userThrowable) {
        runOnUiThread(() -> {
            setFormEnabled(true);
            if (userThrowable != null) {
                Log.e(TAG, "Error fetching user data for biometric login", userThrowable);
                Toast.makeText(this, "Biometric login error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (user != null) {
                Log.d(TAG, "User data retrieved successfully via biometric login: " + user.getUsername());

                AuthManager.getInstance(this).loginUser(user);
                Toast.makeText(this, "Biometric login successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(login_page.this, home_page.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Biometric login failed. Your stored credentials may no longer be valid.",
                    Toast.LENGTH_LONG).show();
            }
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
        try {
            // Clean up BiometricHelper to prevent memory leaks
            if (biometricHelper != null) {
                biometricHelper.cleanup();
                biometricHelper = null;
            }

            // Clean up UserRepository
            if (userRepository != null) {
                userRepository.shutdown();
                userRepository = null;
            }

            // Clear UI references to prevent memory leaks
            emailUsernameBox = null;
            passwordBox = null;
            rememberMeCheckBox = null;
            loginPrefs = null;

            Log.d(TAG, "login_page cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }
}
