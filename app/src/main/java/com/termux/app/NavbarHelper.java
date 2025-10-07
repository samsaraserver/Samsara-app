package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.termux.R;
import com.termux.app.database.managers.AuthManager;

// Security: Add encryption and hashing imports
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class NavbarHelper {

    private static final String TAG = "NavbarHelper";

    public static boolean isUserSignedIn(Context context) {
        AuthManager authManager = AuthManager.getInstance(context);
        return authManager.isLoggedIn();
    }

    public static void setupNavbar(Activity activity) {
        ImageButton homeButton = activity.findViewById(R.id.navbar_home);
        ImageButton configButton = activity.findViewById(R.id.navbar_config);
        ImageButton terminalButton = activity.findViewById(R.id.navbar_center);
        ImageButton documentsButton = activity.findViewById(R.id.navbar_docs);
        ImageButton accountButton = activity.findViewById(R.id.navbar_profile);

        if (homeButton != null) {
            homeButton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, home_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof home_page)) {
                    activity.finish();
                }
            });
        }

        if (configButton != null) {
            configButton.setOnClickListener(view -> {
                if (isUserSignedIn(activity)) {
                    showSignedInAuthenticationOptions(activity);
                } else {
                    showNonSignedInFallbackOptions(activity);
                }
            });
        }

        if (terminalButton != null) {
            terminalButton.setOnClickListener(view -> {
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

        if (documentsButton != null) {
            documentsButton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, documents_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof documents_page)) {
                    activity.finish();
                }
            });
        }

        if (accountButton != null) {
            accountButton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, profile_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof profile_page)) {
                    activity.finish();
                }
            });
        }
    }

    private static void showSignedInAuthenticationOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Authentication Required")
            .setMessage("Please authenticate to access configuration:")
            .setPositiveButton("Use Biometrics", (dialog, which) -> {
                if (activity instanceof FragmentActivity) {
                    showBiometricPromptForConfig((FragmentActivity) activity);
                } else {
                    showPasswordPrompt(activity);
                }
            })
            .setNegativeButton("Use Password", (dialog, which) -> {
                showPasswordPrompt(activity);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showNonSignedInFallbackOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setPositiveButton("Continue to Config", (dialog, which) -> {
                Intent intent = new Intent(activity, configuration_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showPostAuthenticationOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setPositiveButton("Continue to Config", (dialog, which) -> {
                Intent intent = new Intent(activity, configuration_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showBiometricPromptForConfig(FragmentActivity activity) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to access configuration")
                .setNegativeButtonText("Other Options")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            showOtherOptionsDialog(activity);
                        } else if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                            showPasswordPrompt(activity);
                        } else {
                            showSignedInAuthenticationOptions(activity);
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        showPostAuthenticationOptions(activity);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(activity, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private static void showOtherOptionsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Other Options")
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showPasswordPrompt(Activity activity) {
        android.widget.EditText passwordInput = new android.widget.EditText(activity);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Enter password");

        new AlertDialog.Builder(activity)
            .setTitle("Password Authentication")
            .setMessage("Enter your password to access configuration:")
            .setView(passwordInput)
            .setPositiveButton("Authenticate", (dialog, which) -> {
                String password = passwordInput.getText().toString();
                if (validatePassword(activity, password)) {
                    showPostAuthenticationOptions(activity);
                } else {
                    Toast.makeText(activity, "Incorrect password", Toast.LENGTH_SHORT).show();
                    showSignedInAuthenticationOptions(activity);
                }
            })
            .setNegativeButton("Other Options", (dialog, which) -> {
                showOtherOptionsDialog(activity);
            })
            .setNeutralButton("Cancel", (dialog, which) -> {
                showSignedInAuthenticationOptions(activity);
            })
            .show();
    }

    private static boolean validatePassword(Activity activity, String password) {
        AuthManager authManager = AuthManager.getInstance(activity);

        if (authManager.isLoggedIn()) {
            if (authManager.validatePassword(password)) {
                return true;
            }

            android.content.SharedPreferences prefs = activity.getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);

            String customPassword = prefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (customPassword != null) {
                // Security: Use secure password comparison
                return securePasswordCompare(password, customPassword);
            }

            android.content.SharedPreferences oldPrefs = activity.getSharedPreferences("SamsaraAuth", android.content.Context.MODE_PRIVATE);
            String oldCustomPassword = oldPrefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (oldCustomPassword != null) {
                return securePasswordCompare(password, oldCustomPassword);
            }

            return false;
        } else {
            android.content.SharedPreferences prefs = activity.getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);

            // Security: Generate secure default password if none exists
            String storedPassword = prefs.getString("config_password", null);
            if (storedPassword == null) {
                storedPassword = generateSecureDefaultPassword();
                prefs.edit().putString("config_password", hashPassword(storedPassword)).apply();
                // Show the generated password to user once
                showGeneratedPasswordDialog(activity, storedPassword);
                return false; // Force user to see the password first
            }

            // Security: Compare against hashed password
            return verifyPassword(password, storedPassword);
        }
    }

    // Security: Generate cryptographically secure default password
    private static String generateSecureDefaultPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, 12);
    }

    // Security: Hash password using SHA-256 with salt
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            digest.update(salt);
            byte[] hashedPassword = digest.digest(password.getBytes());

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Security: Verify password against hash
    private static boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);
            if (combined.length < 16) return false;

            byte[] salt = new byte[16];
            byte[] hash = new byte[combined.length - 16];
            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, hash, 0, hash.length);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] testHash = digest.digest(password.getBytes());

            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Security: Constant-time password comparison
    private static boolean securePasswordCompare(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // Security: Show generated password dialog
    private static void showGeneratedPasswordDialog(Activity activity, String password) {
        new AlertDialog.Builder(activity)
            .setTitle("Security Notice")
            .setMessage("A secure configuration password has been generated:\n\n" + password +
                       "\n\nPlease save this password securely. You can change it in settings.")
            .setPositiveButton("I've Saved It", null)
            .setCancelable(false)
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

    // Security: Safe navigation helper method
    public static void navigateToActivity(Activity currentActivity, Class<?> targetActivity) {
        try {
            Intent intent = new Intent(currentActivity, targetActivity);
            currentActivity.startActivity(intent);
            currentActivity.finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to activity: " + targetActivity.getSimpleName(), e);
        }
    }
}
