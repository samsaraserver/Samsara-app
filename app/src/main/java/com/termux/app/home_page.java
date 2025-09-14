package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.Html;

import com.termux.R;

public class home_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        ImageButton terminalbutton = findViewById(R.id.imageButton9);
        
        NavbarHelper.setupNavbar(this);

        TextView textView3 = findViewById(R.id.textView3);

        textView3.setText(Html.fromHtml(textView3.getText().toString()));

        terminalbutton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });

    }
}
