package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.termux.R;

public class SamsaraStartupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samsara_startup);

        Button startServerButton = findViewById(R.id.start_server_button);
        View mainContent = findViewById(R.id.main_content);
        View loadingOverlay = findViewById(R.id.loading_overlay);

        startServerButton.setOnClickListener(view -> {
            mainContent.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.VISIBLE);

            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, TermuxActivity.class);
                intent.putExtra("samsara_mode", true);
                startActivity(intent);
                finish();
            }, 800);
        });
    }
}