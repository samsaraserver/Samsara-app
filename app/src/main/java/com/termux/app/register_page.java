package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;

import com.termux.R;
public class register_page extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        TextView tvSignUp2 = findViewById(R.id.tvSignUp2);
        tvSignUp2.setText(Html.fromHtml(tvSignUp2.getText().toString()));

        ImageButton backButton = findViewById(R.id.SignInBtn);
        backButton.setOnClickListener(view -> {
            // Navigate back to login page
            Intent intent = new Intent(register_page.this, login_page.class);
            startActivity(intent);
            finish();
        });
    }
}
