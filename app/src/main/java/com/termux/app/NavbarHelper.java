package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.termux.R;

public class NavbarHelper {

    private static final String TAG = "NavbarHelper";

    public static void setupNavbar(Activity activity) {
        ImageButton homebutton = activity.findViewById(R.id.navbar_home);
        ImageButton configbutton = activity.findViewById(R.id.navbar_config);
        ImageButton terminalbutton = activity.findViewById(R.id.navbar_center);
        ImageButton documentsbutton = activity.findViewById(R.id.navbar_docs);
        ImageButton accountbutton = activity.findViewById(R.id.navbar_profile);

        if (homebutton != null) {
            homebutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, home_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof home_page)) {
                    activity.finish();
                }
            });
        }

        if (configbutton != null) {
            configbutton.setOnClickListener(view -> {
                Log.d(TAG, "Config button clicked - starting biometric authentication process");

                // Check if activity is FragmentActivity for biometric support
                if (activity instanceof FragmentActivity) {
                    FragmentActivity fragmentActivity = (FragmentActivity) activity;
                    Log.d(TAG, "Activity is FragmentActivity, proceeding with biometric setup");

                    // Create BiometricHelper with callback
                    BiometricHelper biometricHelper = new BiometricHelper(fragmentActivity, new BiometricHelper.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                            Log.d(TAG, "Biometric authentication successful, opening config page");
                            // Navigate to config page on successful authentication
                            Intent intent = new Intent(activity, configuration_page.class);
                            activity.startActivity(intent);
                            activity.finish();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            Log.d(TAG, "Biometric authentication failed - user didn't authenticate correctly");
                            Toast.makeText(activity, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                            // Don't show fallback options on failed authentication, let user try again
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            Log.d(TAG, "Biometric authentication error - Code: " + errorCode + ", Message: " + errString);

                            // Only show fallback for certain error codes
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                Log.d(TAG, "User canceled biometric authentication, showing fallback options");
                                showConfigFallbackOptions(activity);
                            } else if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                                Log.d(TAG, "No stored credentials found, showing fallback options");
                                Toast.makeText(activity, "No biometric credentials found. Please log in first to enable biometric access.", Toast.LENGTH_LONG).show();
                                showConfigFallbackOptions(activity);
                            } else {
                                Log.d(TAG, "Biometric authentication not available due to error, showing fallback options");
                                showConfigFallbackOptions(activity);
                            }
                        }
                    });

                    // Check if biometrics are available and start authentication
                    boolean biometricAvailable = biometricHelper.isBiometricAvailable();
                    boolean hasCredentials = biometricHelper.hasStoredCredentials();

                    Log.d(TAG, "Biometric check - Available: " + biometricAvailable + ", Has credentials: " + hasCredentials);

                    if (biometricAvailable) {
                        if (hasCredentials) {
                            Log.d(TAG, "Starting biometric authentication - all conditions met");
                            // Show biometric prompt
                            biometricHelper.startBiometricAuthentication();
                        } else {
                            Log.d(TAG, "No stored credentials, showing fallback");
                            // No stored credentials, show fallback
                            Toast.makeText(activity, "No biometric credentials found. Please log in first to enable biometric access.", Toast.LENGTH_LONG).show();
                            showConfigFallbackOptions(activity);
                        }
                    } else {
                        Log.d(TAG, "Biometrics not available, showing fallback");
                        // Biometrics not available, show fallback
                        Toast.makeText(activity, "Biometric authentication not available on this device.", Toast.LENGTH_SHORT).show();
                        showConfigFallbackOptions(activity);
                    }
                } else {
                    // Activity is not FragmentActivity, show fallback
                    Log.w(TAG, "Activity is not FragmentActivity, cannot use biometrics - showing fallback");
                    showConfigFallbackOptions(activity);
                }
            });
        }

        if (terminalbutton != null) {
            terminalbutton.setOnClickListener(view -> {
                String[] options = new String[]{"Termux (port 333)", "Alpine (port 222)"};
                new AlertDialog.Builder(activity)
                        .setTitle("Select environment")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                launchTermux(activity);
                            } else if (which == 1) {
                                startAlpineSession(activity);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        }

        if (documentsbutton != null) {
            documentsbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, documents_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof documents_page)) {
                    activity.finish();
                }
            });
        }

        if (accountbutton != null) {
            accountbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, profile_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof profile_page)) {
                    activity.finish();
                }
            });
        }
    }

    private static void showConfigFallbackOptions(Activity activity) {
        // Show dialog with config options when biometrics are not available
        new AlertDialog.Builder(activity)
                .setTitle("Configuration Access")
                .setMessage("Choose how to access configuration:")
                .setNegativeButton("Online Wiki", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                    activity.startActivity(browserIntent);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private static void launchTermux(Activity activity) {
        Intent intent = new Intent(activity, TermuxActivity.class);
        intent.setAction(SamsaraIntents.ACTION_SAMSARA_OPEN_TERMUX);
        SamsaraIntents.putEnv(intent, SamsaraIntents.ENV_TERMUX);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void startAlpineSession(Activity activity) {
        Intent termuxIntent = new Intent(activity, TermuxActivity.class);
        termuxIntent.setAction(SamsaraIntents.ACTION_SAMSARA_OPEN_ALPINE);
        SamsaraIntents.putEnv(termuxIntent, SamsaraIntents.ENV_ALPINE);
        activity.startActivity(termuxIntent);
        activity.finish();
    }
}
