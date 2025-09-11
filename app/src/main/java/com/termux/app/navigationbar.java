package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
// Remove the problematic import
// import static android.os.Build.VERSION_CODES.R;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class navigationbar extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the fully qualified path to the R class
        setContentView(com.termux.R.layout.navigationbar);

        setupNavigation();
    }



    public void setupNavigation() {
        setupCenterButton();
        setupFirstButton();
        setupSecondButton();
        setupThirdButton();
        setupFourthButton();
    }

    private void setupCenterButton() {
        ImageButton firstButton = findViewById(com.termux.R.id.imageButton);
        if (firstButton != null) {
            firstButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Replace with your target activity
                    navigateTo(home_page.class);
                }
            });
        }
    }

    private void setupFirstButton() {
        ImageButton firstButton = findViewById(com.termux.R.id.imageButton2);
        if (firstButton != null) {
            firstButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Replace with your target activity
                    navigateTo(home_page.class);
                }
            });
        }
    }

    private void setupSecondButton() {
        ImageButton secondButton = findViewById(com.termux.R.id.imageButton3);
        if (secondButton != null) {
            secondButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Replace with your target activity
                    navigateTo(configuration_page.class);
                }
            });
        }
    }

    private void setupThirdButton() {
        ImageButton thirdButton = findViewById(com.termux.R.id.imageButton4);
        if (thirdButton != null) {
            thirdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Replace with your target activity
                    navigateTo(documents_page.class);
                }
            });
        }
    }

    private void setupFourthButton() {
        ImageButton fourthButton = findViewById(com.termux.R.id.imageButton5);
        if (fourthButton != null) {
            fourthButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Replace with your target activity
                    navigateTo(accounts_page.class);
                }
            });
        }
    }

    private void navigateTo(Class<? extends Activity> cls) {
        try {
            Intent intent = new Intent(this, cls);
            int currentUserId = getIntent().getIntExtra("userId", -1);
            if (currentUserId != -1) {
                intent.putExtra("userId", currentUserId);
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(
                this,
                "Error navigating to " + cls.getSimpleName() + ": " + e.getMessage(),
                Toast.LENGTH_SHORT
            ).show();
        }
    }
}
