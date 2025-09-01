package com.termux.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.termux.R;

public class SamsaraStartupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samsara_startup);

        Button startServerButton = findViewById(R.id.start_server_button);

        startServerButton.setOnClickListener(view -> {
            setContentView(R.layout.login_page);
        });
    }
}
