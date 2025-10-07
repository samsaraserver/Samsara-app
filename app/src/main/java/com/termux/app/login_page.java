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

        biometricHelper = new BiometricHelper(this, new BiometricHelper.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                loginWithStoredCredentials(username, email, password);
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(login_page.this, "Biometric authentication failed. Please try again or use your password.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
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

    }

    private void loadSavedCredentials() {
        boolean rememberMe = loginPrefs.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedUsername = loginPrefs.getString(KEY_EMAIL_USERNAME, "");
            String savedPassword = loginPrefs.getString(KEY_PASSWORD, "");

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
        ImageButton registerButton = findViewById(R.id.RegisterBtn);
        Intent registerIntent = new Intent(login_page.this, register_page.class);
        registerButton.setOnClickListener(view -> {
            startActivity(registerIntent);
            finish();
        });

        ImageButton registerSecondaryButton = findViewById(R.id.RegisterBtn2);
        Intent registerIntent2 = new Intent(login_page.this, register_page.class);
        registerSecondaryButton.setOnClickListener(view -> {
            startActivity(registerIntent2);
            finish();
        });

        ImageButton biometricLoginButton = findViewById(R.id.LoginBiometricsBtn);
        biometricLoginButton.setOnClickListener(view -> {
            if (biometricHelper != null) {
                if (biometricHelper.isBiometricAvailable() && biometricHelper.hasStoredCredentials()) {
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
            ImageButton biometricLoginButton = findViewById(R.id.LoginBiometricsBtn);
            if (biometricLoginButton != null) {
                biometricLoginButton.setEnabled(false);
                biometricLoginButton.setAlpha(0.5f);
            }
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

        new Thread(() -> {
            try {
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
                if (rememberMeCheckBox.isChecked()) {
                    saveCredentials(emailOrUsername, password);
                } else {
                    clearSavedCredentials();
                }

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

        if (biometricHelper.isBiometricAvailable()) {
            String username = user.getUsername();
            String email = user.getEmail();
            String userId = String.valueOf(user.getId());

            biometricHelper.storeCredentials(username, email, password, userId);
            Toast.makeText(this, "Biometric login updated and ready to use!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithStoredCredentials(String username, String email, String password) {
        setFormEnabled(false);
        Toast.makeText(this, "Logging in with biometrics...", Toast.LENGTH_SHORT).show();

        final String loginIdentifier = !TextUtils.isEmpty(email) ? email : username;

        if (TextUtils.isEmpty(loginIdentifier) || TextUtils.isEmpty(password)) {
            Log.e(TAG, "Stored credentials are incomplete");
            Toast.makeText(this, "Stored login information is incomplete. Please log in with password.",
                Toast.LENGTH_LONG).show();
            setFormEnabled(true);
            return;
        }

        new Thread(() -> {
            try {
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
            if (biometricHelper != null) {
                biometricHelper.cleanup();
                biometricHelper = null;
            }

            if (userRepository != null) {
                userRepository.shutdown();
                userRepository = null;
            }

            emailUsernameBox = null;
            passwordBox = null;
            rememberMeCheckBox = null;
            loginPrefs = null;
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }
}
