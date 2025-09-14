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

        ImageButton terminalbutton = findViewById(R.id.terminalBtn1);
        ImageButton homebutton = findViewById(R.id.homeBtn);
        ImageButton configbutton = findViewById(R.id.configBtn);
        ImageButton terminalbutton2 = findViewById(R.id.terminalBtn2);
        ImageButton documentsbutton = findViewById(R.id.docsBtn);
        ImageButton profilebutton = findViewById(R.id.profileBtn);

        TextView textView3 = findViewById(R.id.tvIP);

        // Apply HTML formatting to textView3 to render bold and underline
        textView3.setText(Html.fromHtml(textView3.getText().toString()));

        terminalbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        homebutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, home_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        configbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, configuration_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        terminalbutton2.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        documentsbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, documents_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

        profilebutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, profile_page.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

    }
}
