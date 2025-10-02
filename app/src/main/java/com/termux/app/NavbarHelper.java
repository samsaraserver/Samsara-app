package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.ImageButton;

import com.termux.R;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

public class NavbarHelper {

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
                Intent intent = new Intent(activity, configuration_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            });
        }

        if (terminalbutton != null) {
            terminalbutton.setOnClickListener(view -> {
                // #COMPLETION_DRIVE: Assuming a simple dialog choice is acceptable for environment selection
                // #SUGGEST_VERIFY: Validate UX flow on devices and confirm accessibility/readability of the popup
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

    private static void launchTermux(Activity activity) {
        Intent intent = new Intent(activity, TermuxActivity.class);
        SamsaraIntents.putEnv(intent, SamsaraIntents.ENV_TERMUX);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void startAlpineSession(Activity activity) {
        // #COMPLETION_DRIVE: Defer proot-distro invocation to TermuxActivity bootstrap to ensure it's installed
        // #SUGGEST_VERIFY: Confirm no extra failing session appears before bootstrap completes
        Intent termuxIntent = new Intent(activity, TermuxActivity.class);
        SamsaraIntents.putEnv(termuxIntent, SamsaraIntents.ENV_ALPINE);
        activity.startActivity(termuxIntent);
        activity.finish();
    }
}
