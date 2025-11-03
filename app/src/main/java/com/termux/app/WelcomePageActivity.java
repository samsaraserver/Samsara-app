package com.termux.app;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class WelcomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        ImageButton getStartedButton = findViewById(R.id.WelcomeBtn);
        
        getStartedButton.setOnClickListener(view -> {
            NavbarHelper.navigateToActivity(WelcomePageActivity.this, SignInOut_page.class);
        });
    }
}
