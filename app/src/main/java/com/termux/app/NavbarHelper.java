package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageButton;

import com.termux.R;

public class NavbarHelper {
    
    public static void setupNavbar(Activity activity) {
        ImageButton homebutton = activity.findViewById(R.id.navbar1);
        ImageButton configbutton = activity.findViewById(R.id.navbar2);
        ImageButton terminalbutton = activity.findViewById(R.id.navbar3);
        ImageButton documentsbutton = activity.findViewById(R.id.navbar4);
        ImageButton accountbutton = activity.findViewById(R.id.navbar5);
        if (homebutton != null) {
            homebutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, home_page.class);
                intent.putExtra("samsara_mode", true);
                activity.startActivity(intent);
                if (!(activity instanceof home_page)) {
                    activity.finish();
                }
            });
        }

        if (configbutton != null) {
            configbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, configuration_page.class);
                intent.putExtra("samsara_mode", true);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            });
        }

        if (terminalbutton != null) {
            terminalbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, TermuxActivity.class);
                intent.putExtra("samsara_mode", true);
                activity.startActivity(intent);
                activity.finish();
            });
        }

        if (documentsbutton != null) {
            documentsbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, documents_page.class);
                intent.putExtra("samsara_mode", true);
                activity.startActivity(intent);
                if (!(activity instanceof documents_page)) {
                    activity.finish();
                }
            });
        }

        if (accountbutton != null) {
            accountbutton.setOnClickListener(view -> {
                Intent intent = new Intent(activity, profile_page.class);
                intent.putExtra("samsara_mode", true);
                activity.startActivity(intent);
                if (!(activity instanceof profile_page)) {
                    activity.finish();
                }
            });
        }
    }
}