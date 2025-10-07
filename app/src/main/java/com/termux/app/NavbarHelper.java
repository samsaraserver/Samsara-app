package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.termux.R;
import com.termux.app.database.managers.AuthManager;

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
                } else{
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
                return password.equals(customPassword);
            }

            android.content.SharedPreferences oldPrefs = activity.getSharedPreferences("SamsaraAuth", android.content.Context.MODE_PRIVATE);
            String oldCustomPassword = oldPrefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (oldCustomPassword != null) {
                return password.equals(oldCustomPassword);
            }

            return false;
        } else {
            android.content.SharedPreferences prefs = activity.getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);
            String storedPassword = prefs.getString("config_password", "admin123");

            if (!password.equals(storedPassword)) {
                android.content.SharedPreferences oldPrefs = activity.getSharedPreferences("SamsaraAuth", android.content.Context.MODE_PRIVATE);
                String oldStoredPassword = oldPrefs.getString("config_password", "admin123");
                return password.equals(oldStoredPassword);
            }

            return password.equals(storedPassword);
        }
    }

    public static void setConfigPassword(android.content.Context context, String newPassword) {
        AuthManager authManager = AuthManager.getInstance(context);
        android.content.SharedPreferences prefs = context.getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);

        if (authManager.isLoggedIn()) {
            prefs.edit().putString("user_config_password_" + authManager.getCurrentUser().getUsername(), newPassword).apply();
        } else {
            prefs.edit().putString("config_password", newPassword).apply();
        }
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
