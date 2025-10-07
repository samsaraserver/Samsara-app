package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.termux.R;

public class NavbarHelper {

    private static final String TAG = "NavbarHelper";

    public static void setupNavbar(Activity activity) {
        ImageButton homeButton = activity.findViewById(R.id.navbar_home);
        ImageButton configButton = activity.findViewById(R.id.navbar_config);
        ImageButton terminalButton = activity.findViewById(R.id.navbar_center);
        ImageButton documentsButton = activity.findViewById(R.id.navbar_docs);
        ImageButton accountButton = activity.findViewById(R.id.navbar_profile);

        if (homeButton != null) {
            homeButton.setOnClickListener(view -> navigateToActivity(activity, home_page.class));
        }

        if (configButton != null) {
            configButton.setOnClickListener(view -> {
                if (activity instanceof FragmentActivity) {
                    showBiometricPromptForConfig((FragmentActivity) activity);
                } else {
                    showConfigFallbackOptions(activity);
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
            documentsButton.setOnClickListener(view -> navigateToActivity(activity, documents_page.class));
        }

        if (accountButton != null) {
            accountButton.setOnClickListener(view -> navigateToActivity(activity, profile_page.class));
        }
    }

    public static void navigateToActivity(Activity activity, Class<? extends Activity> targetClass) {
        if (activity == null || targetClass == null) {
            return;
        }

        if (activity.getClass() == targetClass) {
            return;
        }

        Intent intent = new Intent(activity, targetClass);
        // #COMPLETION_DRIVE: Assuming reordering the existing target activity maintains history without spawning duplicates
        // #SUGGEST_VERIFY: Navigate between pages repeatedly and confirm the back button steps through prior pages instead of closing the app
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static void showBiometricPromptForConfig(FragmentActivity activity) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to access configuration")
                .setNegativeButtonText("Use fallback")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        showConfigFallbackOptions(activity);
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        navigateToActivity(activity, configuration_page.class);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(activity, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private static void showConfigFallbackOptions(Activity activity) {
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
        // #COMPLETION_DRIVE: Assuming reordering Termux keeps previous UI screens accessible when returning
        // #SUGGEST_VERIFY: Launch Termux from multiple pages and confirm back returns to originating screen instead of closing the task
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static void startAlpineSession(Activity activity) {
        Intent termuxIntent = new Intent(activity, TermuxActivity.class);
        termuxIntent.setAction(SamsaraIntents.ACTION_SAMSARA_OPEN_ALPINE);
        SamsaraIntents.putEnv(termuxIntent, SamsaraIntents.ENV_ALPINE);
        // #COMPLETION_DRIVE: Assuming bringing Alpine session forward reuses the existing Termux activity context without duplicating it
        // #SUGGEST_VERIFY: Switch between Termux and Alpine options, then inspect activity stack with adb to ensure only one instance exists
        termuxIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(termuxIntent);
    }
}
