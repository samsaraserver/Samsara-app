package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.termux.R;

public class SamsaraStartupActivity extends Activity {

    private boolean mTerminalLaunchStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samsara_startup);

        Button startServerButton = findViewById(R.id.start_server_button);
        View mainContent = findViewById(R.id.main_content);
        View loadingOverlay = findViewById(R.id.loading_overlay);

        startServerButton.setOnClickListener(view -> {
            if (mainContent != null) mainContent.setVisibility(View.GONE);
            if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mTerminalLaunchStarted) return;
                mTerminalLaunchStarted = true;
                Intent intent = new Intent(SamsaraStartupActivity.this, TermuxActivity.class);
                intent.putExtra("samsara_mode", true);
                startActivity(intent);
                finish();
            }, 800);

            setContentView(R.layout.login_page);
            ImageButton loginStartButton = findViewById(R.id.Imagebutton);
            if (loginStartButton != null) {
                loginStartButton.setOnClickListener(v -> {
                    if (mTerminalLaunchStarted) return;
                    mTerminalLaunchStarted = true;
                    Intent intent = new Intent(SamsaraStartupActivity.this, TermuxActivity.class);
                    //intent.putExtra("samsara_mode", true);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}
