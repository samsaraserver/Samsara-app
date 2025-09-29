package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
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

        ImageButton SignIn = findViewById(R.id.SignInBtn);
        SignIn.setOnClickListener(view -> {
            // Navigate back to login page
            Intent intent = new Intent(register_page.this, login_page.class);
            startActivity(intent);
            finish();
        });

        Button SignIn2 = findViewById(R.id.SignInBtn2);
        SignIn2.setOnClickListener(view -> {
            // Navigate back to login page
            Intent intent = new Intent(register_page.this, login_page.class);
            startActivity(intent);
            finish();
        });

    }
}
