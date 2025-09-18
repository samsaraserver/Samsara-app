package com.termux.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.termux.R;

public class configuration_page extends Activity {
    private Spinner spinnerConnectionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_page);

        NavbarHelper.setupNavbar(this);
        SetupSpinner();
    }

    private void SetupSpinner() {
        spinnerConnectionType = findViewById(R.id.spinnerMonitoringInterval);

        String[] options = {"15s", "30s", "1m", "5m", "10m", "30m", "1h"};

        // Create a custom adapter for both normal view and dropdown view
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                // Set solid background color to match the drawable color (#292929)
                view.setBackgroundColor(0xFF292929);
                // Set text color to white for better visibility
                ((android.widget.TextView) view).setTextColor(0xFFFFFFFF);
                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // Set text color to white for the initial display
                ((android.widget.TextView) view).setTextColor(0xFFFFFFFF);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerConnectionType.setAdapter(adapter);


        // Set selection listener
        spinnerConnectionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = options[position];
                // Handle selection
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle no selection
            }
        });
    }
}
