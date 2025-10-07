package com.termux.app;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.termux.R;
import com.termux.AlpineFragment;
import com.termux.OverviewFragment;
import com.termux.SetupFragment;

public class documents_page extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.documents_page);

        NavbarHelper.setupNavbar(this);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            OverviewFragment overviewFragment = new OverviewFragment();
            fragmentTransaction.replace(R.id.fragment_container, overviewFragment, "overview");
            fragmentTransaction.commit();
        }

        ImageButton fragmentOverviewButton = findViewById(R.id.Fragment1Btn);
        fragmentOverviewButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            OverviewFragment overviewFragment = new OverviewFragment();
            fragmentTransaction.replace(R.id.fragment_container, overviewFragment, "overview");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        ImageButton fragmentSetupButton = findViewById(R.id.Fragment2Btn);
        fragmentSetupButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SetupFragment setupFragment = new SetupFragment();
            fragmentTransaction.replace(R.id.fragment_container, setupFragment, "setup");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        ImageButton fragmentAlpineButton = findViewById(R.id.Fragment3Btn);
        fragmentAlpineButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            AlpineFragment alpineFragment = new AlpineFragment();
            fragmentTransaction.replace(R.id.fragment_container, alpineFragment, "alpine");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
    }
}
