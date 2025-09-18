package com.termux.app;

import android.app.Activity;
import android.os.Bundle;

import com.termux.R;

public class biometrics_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometrics_page);

        NavbarHelper.setupNavbar(this);
    }
}
