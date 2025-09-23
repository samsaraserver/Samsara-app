package com.termux.app;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.termux.R;
import com.termux.alpine_fragment;
import com.termux.overview_fragment;
import com.termux.setup_fragment;

public class documents_page extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.documents_page);

        NavbarHelper.setupNavbar(this);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            overview_fragment overviewFragment = new overview_fragment();
            fragmentTransaction.replace(R.id.fragment_container, overviewFragment, "overview");
            fragmentTransaction.commit();
        }

        ImageButton Fragment1 = findViewById(R.id.Fragment1Btn);
        Fragment1.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            overview_fragment overviewFragment = new overview_fragment();
            fragmentTransaction.replace(R.id.fragment_container, overviewFragment, "overview");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        ImageButton Fragment2 = findViewById(R.id.Fragment2Btn);
        Fragment2.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            setup_fragment setup_fragment = new setup_fragment();
            fragmentTransaction.replace(R.id.fragment_container, setup_fragment, "overview");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        ImageButton Fragment3 = findViewById(R.id.Fragment3Btn);
        Fragment3.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            alpine_fragment alpine_fragment = new alpine_fragment();
            fragmentTransaction.replace(R.id.fragment_container, alpine_fragment, "overview");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
    }
}
