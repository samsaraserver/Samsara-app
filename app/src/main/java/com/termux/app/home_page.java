package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.Html;

import com.termux.R;

public class home_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);


        NavbarHelper.setupNavbar(this);

        //TextView textViewUnderline = findViewById(R.id.tvIP);

        //String ipAddress = getDeviceIpAddress(); // Implement this method
        //textViewUnderline.setText(Html.fromHtml("<u>" + ipAddress + "</u>"));

        ImageButton monitorBtn = findViewById(R.id.monitorBtn);

        monitorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, monitor_page.class);
            // #COMPLETION_DRIVE: Remove legacy samsara_mode flag; monitor page should not depend on it
            // #SUGGEST_VERIFY: Open monitor page from home and verify no regressions in behavior
            startActivity(intent);
            finish();
        });

        ImageButton configurationBtn = findViewById(R.id.configurationBtn);

        configurationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, configuration_page.class);
            // #COMPLETION_DRIVE: Remove legacy samsara_mode flag; configuration page should not depend on it
            // #SUGGEST_VERIFY: Open configuration page from home and verify expected defaults load
            startActivity(intent);
            finish();
        });

        ImageButton ProjectBtn = findViewById(R.id.ProjectBtn);

        ProjectBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, projects_page.class);
            // #COMPLETION_DRIVE: Remove legacy samsara_mode flag; projects page should not depend on it
            // #SUGGEST_VERIFY: Open projects page from home and verify UI loads
            startActivity(intent);
            finish();
        });

        ImageButton documentationBtn = findViewById(R.id.documentationBtn);

        documentationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, documents_page.class);
            // #COMPLETION_DRIVE: Remove legacy samsara_mode flag; docs page should not depend on it
            // #SUGGEST_VERIFY: Open docs page from home and ensure content displays
            startActivity(intent);
            finish();
        });

        ImageButton terminalbutton = findViewById(R.id.terminalBtn2);

        terminalbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            SamsaraIntents.putEnv(intent, SamsaraIntents.ENV_TERMUX);
            startActivity(intent);
            finish();
        });
    }
}
