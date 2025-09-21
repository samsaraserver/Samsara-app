package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;

import com.termux.R;

public class login_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setText(Html.fromHtml(tvForgotPassword.getText().toString()));

        TextView tvSignUp2 = findViewById(R.id.tvSignUp2);
        tvSignUp2.setText(Html.fromHtml(tvSignUp2.getText().toString()));

        ImageButton SignUpButton = findViewById(R.id.SignUpBtn);
        Intent intent = new Intent(login_page.this, register_page.class);
        SignUpButton.setOnClickListener(view -> {
            startActivity(intent);
            finish();
        });

        ImageButton login2Button = findViewById(R.id.ContinueWithoutAccountBtn);
        Intent intent2 = new Intent(login_page.this, home_page.class);
        login2Button.setOnClickListener(view -> {
            startActivity(intent2);
            finish();
        });

    }
}
