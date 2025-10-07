package com.termux.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageButton;

import com.termux.R;

public class WelcomePageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        ImageButton getStartedButton = findViewById(R.id.WelcomeBtn);
        
        getStartedButton.setOnClickListener(view -> {
            NavbarHelper.navigateToActivity(WelcomePageActivity.this, login_page.class);
        });
    }
}
