package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.termux.R;
import java.util.concurrent.Executor;
import android.app.AlertDialog;

public class biometrics_page extends FragmentActivity {

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ImageButton biometricsButton;
    private ImageButton cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometrics_page);

        initializeViews();
        setupBiometrics();
        setupClickListeners();
        checkBiometricAvailability();
    }

    private void initializeViews() {
        biometricsButton = findViewById(R.id.BiometricsBtn);
        cancelButton = findViewById(R.id.CancelBtn);
    }

    private void setupBiometrics() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
            new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    showToast("Authentication error: " + errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    showToast("Authentication succeeded!");
                    handleSuccessfulAuthentication();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    showToast("Authentication failed");
                }
            });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your biometric credential to authenticate")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void setupClickListeners() {
        biometricsButton.setOnClickListener(v -> startBiometricAuthentication());

        cancelButton.setOnClickListener(v -> {
            showToast("Biometric authentication cancelled");
            finish();
        });
    }

    private void checkBiometricAvailability() {
        BiometricManager biometricManager = BiometricManager.from(this);

        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Biometric features are available
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                showToast("No biometric features available on this device");
                biometricsButton.setEnabled(false);
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                showToast("Biometric features are currently unavailable");
                biometricsButton.setEnabled(false);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                showToast("No biometric credentials are enrolled");
                showEnrollmentDialog();
                biometricsButton.setEnabled(false);
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                showToast("Security update required for biometric authentication");
                biometricsButton.setEnabled(false);
                break;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                showToast("Biometric authentication is not supported");
                biometricsButton.setEnabled(false);
                break;
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                showToast("Biometric status unknown");
                biometricsButton.setEnabled(false);
                break;
            default:
                showToast("Unknown biometric status");
                biometricsButton.setEnabled(false);
                break;
        }
    }

    private void startBiometricAuthentication() {
        biometricPrompt.authenticate(promptInfo);
    }

    private void handleSuccessfulAuthentication() {
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showEnrollmentDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Enroll Biometrics")
            .setMessage("To use biometric authentication, please enroll your biometrics in the device settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                // Open the device settings for biometric enrollment
                Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
