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

        ImageButton startServerButton = findViewById(R.id.imageButton9);
        TextView textView3 = findViewById(R.id.textView3);

        // Apply HTML formatting to textView3 to render bold and underline
        textView3.setText(Html.fromHtml(textView3.getText().toString()));

        startServerButton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, TermuxActivity.class);
            intent.putExtra("samsara_mode", true);
            startActivity(intent);
            finish();
        });
    }
}
