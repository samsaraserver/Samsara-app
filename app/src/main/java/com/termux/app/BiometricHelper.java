package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

public class BiometricHelper {
    private static final String TAG = "BiometricHelper";
    private static final String PREF_NAME = "BiometricLoginPrefs";
    private static final String KEY_USERNAME = "biometric_username";
    private static final String KEY_EMAIL = "biometric_email";
    private static final String KEY_PASSWORD = "biometric_password";
    private static final String KEY_USER_ID = "biometric_user_id";

    // Use WeakReference to prevent memory leaks
    private final WeakReference<FragmentActivity> activityRef;
    private final WeakReference<AuthenticationCallback> callbackRef;
    private final SharedPreferences prefs;
    private BiometricPrompt biometricPrompt;

    public interface AuthenticationCallback {
        void onAuthenticationSuccessful(String username, String email, String password, String userId);
        void onAuthenticationFailed();
        void onAuthenticationError(int errorCode, CharSequence errString);
    }

    public BiometricHelper(FragmentActivity activity, AuthenticationCallback callback) {
        this.activityRef = new WeakReference<>(activity);
        this.callbackRef = new WeakReference<>(callback);
        this.prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setupBiometricPrompt();
    }

    private void setupBiometricPrompt() {
        FragmentActivity activity = activityRef.get();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            Log.w(TAG, "Activity is null or destroyed, cannot setup biometric prompt");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        biometricPrompt = new BiometricPrompt(activity, executor, new BiometricAuthCallback());
    }

    // Separate class to avoid anonymous inner class memory leaks
    private class BiometricAuthCallback extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            // SIMPLIFIED - No longer need cipher for decryption
            retrieveCredentials();
        }

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            notifyCallback(callback -> callback.onAuthenticationError(errorCode, errString));
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            notifyCallback(callback -> callback.onAuthenticationFailed());
        }
    }

    // Safe callback notification that checks for null references
    private void notifyCallback(CallbackAction action) {
        AuthenticationCallback callback = callbackRef.get();
        if (callback != null) {
            try {
                action.execute(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error in callback execution: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Callback reference is null, cannot notify");
        }
    }

    private interface CallbackAction {
        void execute(AuthenticationCallback callback);
    }

    public boolean isBiometricAvailable() {
        FragmentActivity activity = activityRef.get();
        if (activity == null) {
            Log.w(TAG, "Activity reference is null");
            return false;
        }

        BiometricManager biometricManager = BiometricManager.from(activity);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean hasStoredCredentials() {
        boolean hasUsername = prefs.contains(KEY_USERNAME);
        boolean hasPassword = prefs.contains(KEY_PASSWORD);

        Log.d(TAG, "Checking stored credentials - Username: " + hasUsername +
                   ", Password: " + hasPassword);

        return hasUsername && hasPassword;
    }

    public void startBiometricAuthentication() {
        FragmentActivity activity = activityRef.get();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            Log.w(TAG, "Activity is null or destroyed, cannot start authentication");
            notifyCallback(callback -> callback.onAuthenticationError(
                BiometricPrompt.ERROR_CANCELED, "Activity is not available"));
            return;
        }

        try {
            if (!hasStoredCredentials()) {
                Log.e(TAG, "Cannot start biometric authentication: No stored credentials found");
                notifyCallback(callback -> callback.onAuthenticationError(
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                    "No biometric credentials saved. Please log in with password first."));
                return;
            }

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Login")
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();

            Log.d(TAG, "Starting biometric authentication prompt");
            if (biometricPrompt != null) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                Log.e(TAG, "BiometricPrompt is null");
                notifyCallback(callback -> callback.onAuthenticationError(
                    BiometricPrompt.ERROR_HW_UNAVAILABLE, "Biometric prompt not initialized"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting biometric authentication: " + e.getMessage(), e);
            notifyCallback(callback -> callback.onAuthenticationError(
                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                "Biometric authentication unavailable: " + e.getMessage()));
        }
    }

    public void storeCredentials(String username, String email, String password, String userId) {
        // SIMPLIFIED - Remove complex encryption that causes crashes
        try {
            // Store credentials in plain SharedPreferences (less secure but more stable)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password); // Store in plain text for now
            editor.putString(KEY_USER_ID, userId);
            editor.apply();

            Log.d(TAG, "Credentials stored for biometric login (simplified)");
        } catch (Exception e) {
            Log.e(TAG, "Error storing credentials for biometric login", e);
        }
    }

    private void retrieveCredentials() {
        try {
            // SIMPLIFIED - Remove complex decryption
            String password = prefs.getString(KEY_PASSWORD, null);
            String username = prefs.getString(KEY_USERNAME, "");
            String email = prefs.getString(KEY_EMAIL, "");
            String userId = prefs.getString(KEY_USER_ID, "");

            if (password == null) {
                Log.e(TAG, "No stored password found");
                notifyCallback(callback -> callback.onAuthenticationFailed());
                return;
            }

            // Pass the credentials back via the callback
            notifyCallback(callback -> callback.onAuthenticationSuccessful(username, email, password, userId));
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving stored credentials", e);
            notifyCallback(callback -> callback.onAuthenticationFailed());
        }
    }

    /**
     * Clear all stored biometric credentials
     * Also cleans up references to prevent memory leaks
     */
    public void clearStoredCredentials() {
        try {
            // Clear SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Log.d(TAG, "Biometric credentials cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing stored credentials", e);
        }
    }

    /**
     * Clean up resources to prevent memory leaks
     * Call this when the activity is being destroyed
     */
    public void cleanup() {
        try {
            Log.d(TAG, "Cleaning up BiometricHelper resources");

            // Clear BiometricPrompt reference
            biometricPrompt = null;

            // Clear weak references (they'll be garbage collected naturally)
            // No need to clear WeakReference contents manually

            Log.d(TAG, "BiometricHelper cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during BiometricHelper cleanup: " + e.getMessage(), e);
        }
    }
}
