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

        TextView textView3 = findViewById(R.id.tvIP);

        textView3.setText(Html.fromHtml(textView3.getText().toString()));

        ImageButton monitorBtn = findViewById(R.id.monitorBtn);

        monitorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, monitor_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        ImageButton configurationBtn = findViewById(R.id.configurationBtn);

        configurationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, configuration_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        ImageButton ProjectBtn = findViewById(R.id.ProjectBtn);

        ProjectBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, projects_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        ImageButton documentationBtn = findViewById(R.id.documentationBtn);

        documentationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, documents_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        ImageButton terminalbutton = findViewById(R.id.terminalBtn2);

        terminalbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });
    }
}
