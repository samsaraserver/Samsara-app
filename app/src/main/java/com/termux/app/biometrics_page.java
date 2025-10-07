package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.termux.R;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;
import java.util.concurrent.Executor;
import android.app.AlertDialog;

public class biometrics_page extends FragmentActivity {

    private static final String TAG = "BiometricsPage";
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ImageButton biometricsButton;
    private ImageButton cancelButton;
    private BiometricHelper biometricHelper;
    private SamsaraUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.biometrics_page);
            AuthManager authManager = AuthManager.getInstance(this);
            currentUser = authManager.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to set up biometric authentication", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            initializeViews();
            checkBiometricAvailability();
            initializeBiometricHelper();
            setupBiometrics();
            setupClickListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up biometrics page", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            biometricsButton = findViewById(R.id.BiometricsBtn);
            cancelButton = findViewById(R.id.CancelBtn);

            if (biometricsButton == null || cancelButton == null) {
                Log.e(TAG, "Required UI elements not found in layout");
                Toast.makeText(this, "UI setup error", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            finish();
        }
    }

    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);

        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                showToast("No biometric features available on this device.");
                disableBiometricsSetup();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                showToast("Biometric features are currently unavailable.");
                disableBiometricsSetup();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                showBiometricEnrollmentDialog();
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                showToast("Security update required for biometric authentication.");
                disableBiometricsSetup();
                break;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                showToast("Biometric authentication is not supported.");
                disableBiometricsSetup();
                break;
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                showToast("Biometric status unknown.");
                disableBiometricsSetup();
                break;
        }
    }

    private void initializeBiometricHelper() {
        try {
            biometricHelper = new BiometricHelper(this, new BiometricHelper.AuthenticationCallback() {
                @Override
                public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                }

                @Override
                public void onAuthenticationFailed() {
                    showToast("Biometric authentication failed");
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        showToast("Authentication error: " + errString);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BiometricHelper: " + e.getMessage(), e);
            showToast("Error setting up biometric authentication");
            disableBiometricsSetup();
        }
    }

    private void setupBiometrics() {
        try {
            Executor executor = ContextCompat.getMainExecutor(this);

            biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            showToast("Authentication error: " + errString);
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        showToast("Biometric authentication verified!");
                        handleSuccessfulAuthentication();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        showToast("Biometric authentication failed. Try again.");
                    }
                });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Setup Biometric Login")
                    .setSubtitle("Verify your identity to enable biometric login")
                    .setDescription("Place your finger on the sensor or look at the camera to set up biometric authentication for future logins.")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up biometrics: " + e.getMessage(), e);
            showToast("Error setting up biometric prompt");
            disableBiometricsSetup();
        }
    }

    private void setupClickListeners() {
        try {
            if (biometricsButton != null) {
                biometricsButton.setOnClickListener(view -> {
                    try {
                        startBiometricSetup();
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting biometric setup: " + e.getMessage(), e);
                        showToast("Error starting biometric setup");
                    }
                });
            }

            if (cancelButton != null) {
                cancelButton.setOnClickListener(view -> {
                    finish();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void startBiometricSetup() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            showToast("Biometric setup is not properly initialized");
        }
    }

    private void handleSuccessfulAuthentication() {
        try {
            if (biometricHelper != null && currentUser != null) {
                showPasswordDialog();
            } else {
                Log.e(TAG, "BiometricHelper or currentUser is null");
                showToast("Setup error - please try again");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleSuccessfulAuthentication: " + e.getMessage(), e);
            showToast("Error completing biometric setup");
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Password");
        builder.setMessage("Please enter your password to complete biometric setup:");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Setup", (dialog, which) -> {
            String password = input.getText().toString();
            if (!password.isEmpty()) {
                completeBiometricSetup(password);
            } else {
                showToast("Password is required for biometric setup");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void completeBiometricSetup(String password) {
        try {
            if (biometricHelper != null && currentUser != null) {
                String username = currentUser.getUsername();
                String email = currentUser.getEmail();
                String userId = String.valueOf(currentUser.getId());

                biometricHelper.storeCredentials(username, email, password, userId);
                showToast("Biometric login setup complete!");
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error completing biometric setup: " + e.getMessage(), e);
            showToast("Error completing biometric setup");
        }
    }

    private void showBiometricEnrollmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Biometrics Enrolled")
                .setMessage("You need to enroll biometric credentials (fingerprint, face, etc.) in your device settings before you can use biometric login.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                    enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG);
                    try {
                        startActivity(enrollIntent);
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void disableBiometricsSetup() {
        if (biometricsButton != null) {
            biometricsButton.setEnabled(false);
            biometricsButton.setAlpha(0.5f);
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (biometricHelper != null) {
                biometricHelper.cleanup();
                biometricHelper = null;
            }

            biometricPrompt = null;
            promptInfo = null;
            currentUser = null;
            biometricsButton = null;
            cancelButton = null;
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }
}
