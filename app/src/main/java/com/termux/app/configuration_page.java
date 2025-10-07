package com.termux.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class configuration_page extends AppCompatActivity {
    private Spinner spinnerConnectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_page);

        NavbarHelper.setupNavbar(this);
        setupSpinner();
    }

    private void setupSpinner() {
        spinnerConnectionType = findViewById(R.id.spinnerMonitoringInterval);

        String[] options = {"15s", "30s", "1m", "5m", "10m", "30m", "1h"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(0xFF292929);
                ((android.widget.TextView) view).setTextColor(0xFFFFFFFF);
                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((android.widget.TextView) view).setTextColor(0xFFFFFFFF);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConnectionType.setAdapter(adapter);

        spinnerConnectionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = options[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
