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
                Intent intent = new Intent(activity, configuration_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
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
