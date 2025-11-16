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
    private static final String PREF_NAME = "BiometricSignupPrefs";
    private static final String KEY_USERNAME = "biometric_username";
    private static final String KEY_EMAIL = "biometric_email";
    private static final String KEY_PASSWORD = "biometric_password";
    private static final String KEY_USER_ID = "biometric_user_id";
    private static final String KEY_AUTH_PROVIDER = "biometric_auth_provider";
    private static final String OAUTH_PASSWORD_MARKER = "__OAUTH_USER__";

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
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        biometricPrompt = new BiometricPrompt(activity, executor, new BiometricAuthCallback());
    }

    private class BiometricAuthCallback extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
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

    private void notifyCallback(CallbackAction action) {
        AuthenticationCallback callback = callbackRef.get();
        if (callback != null) {
            try {
                action.execute(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error in callback execution: " + e.getMessage(), e);
            }
        }
    }

    private interface CallbackAction {
        void execute(AuthenticationCallback callback);
    }

    public boolean isBiometricAvailable() {
        FragmentActivity activity = activityRef.get();
        if (activity == null) {
            return false;
        }

        BiometricManager biometricManager = BiometricManager.from(activity);

        int canAuthenticateStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (canAuthenticateStrong == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d(TAG, "BIOMETRIC_STRONG available");
            return true;
        }

        int canAuthenticateWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (canAuthenticateWeak == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d(TAG, "BIOMETRIC_WEAK available");
            return true;
        }

        int canAuthenticateWithCredential = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        boolean isAvailable = canAuthenticateWithCredential == BiometricManager.BIOMETRIC_SUCCESS;

        Log.d(TAG, "Biometric availability check - Strong: " + canAuthenticateStrong +
                   ", Weak: " + canAuthenticateWeak +
                   ", With credential: " + canAuthenticateWithCredential +
                   ", Final result: " + isAvailable);

        return isAvailable;
    }

    public boolean hasStoredCredentials() {
        boolean hasUsername = prefs.contains(KEY_USERNAME);
        boolean hasPassword = prefs.contains(KEY_PASSWORD);
        return hasUsername && hasPassword;
    }

    public void startBiometricAuthentication() {
        FragmentActivity activity = activityRef.get();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            notifyCallback(callback -> callback.onAuthenticationError(
                BiometricPrompt.ERROR_CANCELED, "Activity is not available"));
            return;
        }

        try {
            if (!hasStoredCredentials()) {
                notifyCallback(callback -> callback.onAuthenticationError(
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                    "No biometric credentials saved. Please log in with password first."));
                return;
            }

            BiometricManager biometricManager = BiometricManager.from(activity);
            int authenticators;
            String subtitle;

            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
                subtitle = "Use your fingerprint or face to access configuration";
                Log.d(TAG, "Using BIOMETRIC_STRONG");
            } else if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
                authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;
                subtitle = "Use your biometric or device credential";
                Log.d(TAG, "Using BIOMETRIC_WEAK");
            } else {
                authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
                subtitle = "Use your biometric or device PIN/password";
                Log.d(TAG, "Using BIOMETRIC_WEAK with DEVICE_CREDENTIAL");
            }

            BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Configuration Access")
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators);

            if ((authenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 0) {
                promptBuilder.setNegativeButtonText("Cancel");
            }

            BiometricPrompt.PromptInfo promptInfo = promptBuilder.build();

            Log.d(TAG, "Starting biometric authentication prompt with authenticators: " + authenticators);
            if (biometricPrompt != null) {
                biometricPrompt.authenticate(promptInfo);
            } else {
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
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putString(KEY_USER_ID, userId);
            // Do not overwrite auth provider for regular password storage
            boolean success = editor.commit(); // synchronous commit to ensure data is available immediately
            if (success) {
                Log.d(TAG, "Successfully stored biometric credentials for user: " + username);
                String storedUsername = prefs.getString(KEY_USERNAME, null);
                String storedPassword = prefs.getString(KEY_PASSWORD, null);
                Log.d(TAG, "Verification - Username stored: " + (storedUsername != null) + ", Password stored: " + (storedPassword != null));
            } else {
                Log.e(TAG, "Failed to commit biometric credentials to SharedPreferences!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing credentials for biometric signup", e);
        }
    }

    public void storeOAuthUserCredentials(String username, String email, String userId, String authProvider) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, OAUTH_PASSWORD_MARKER);
            editor.putString(KEY_USER_ID, userId);
            if (authProvider != null) {
                editor.putString(KEY_AUTH_PROVIDER, authProvider);
            }
            boolean success = editor.commit();
            if (success) {
                Log.d(TAG, "Successfully stored OAuth biometric credentials for user: " + username + " (provider: " + authProvider + ")");
            } else {
                Log.e(TAG, "Failed to commit OAuth biometric credentials to SharedPreferences!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing OAuth credentials for biometric signup", e);
        }
    }

    public boolean isOAuthUser() {
        String authProvider = prefs.getString(KEY_AUTH_PROVIDER, null);
        return authProvider != null && !authProvider.isEmpty();
    }

    private void retrieveCredentials() {
        try {
            String password = prefs.getString(KEY_PASSWORD, null);
            String username = prefs.getString(KEY_USERNAME, "");
            String email = prefs.getString(KEY_EMAIL, "");
            String userId = prefs.getString(KEY_USER_ID, "");

            if (password == null) {
                notifyCallback(callback -> callback.onAuthenticationFailed());
                return;
            }

            notifyCallback(callback -> callback.onAuthenticationSuccessful(username, email, password, userId));
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving stored credentials", e);
            notifyCallback(callback -> callback.onAuthenticationFailed());
        }
    }

    public void clearStoredCredentials() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing stored credentials", e);
        }
    }

    public void cleanup() {
        try {
            biometricPrompt = null;
        } catch (Exception e) {
            Log.e(TAG, "Error during BiometricHelper cleanup: " + e.getMessage(), e);
        }
    }
}
