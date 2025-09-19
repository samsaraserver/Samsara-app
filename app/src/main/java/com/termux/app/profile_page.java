package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import com.termux.R;

public class profile_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        NavbarHelper.setupNavbar(this);

        ImageButton button2 = findViewById(R.id.SetupBiometricsBtn);
        button2.setOnClickListener(v -> {
            Intent intent = new Intent(profile_page.this, biometrics_page.class);
            startActivity(intent);
        });
    }
}
