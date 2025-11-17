package com.termux.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.termux.R;
import com.termux.app.database.managers.ConfigManager;

public class configuration_page extends AppCompatActivity {
    // UI Elements
    private SwitchCompat autoStartOnBoot;
    private EditText webPortBox;
    private EditText sshPortBox;
    private Spinner spinnerMonitoringInterval;
    private EditText tempBox;
    private EditText lowStorageBox;
    private ImageButton resetBtn;
    private ImageButton saveBtn;
    private TextView tvAppVersion2;
    private TextView tvBusyBox2;
    private ImageButton systemFilesBtn;

    private ConfigManager configManager;
    private String[] monitoringIntervalOptions = {"15s", "30s", "1m", "5m", "10m", "30m", "1h"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_page);

        NavbarHelper.setupNavbar(this);

        // Initialize ConfigManager
        configManager = ConfigManager.getInstance(this);

        // Initialize all UI elements
        initializeUIElements();

        // Setup spinner
        setupSpinner();

        // Load saved settings
        loadSettings();

        // Setup button listeners
        setupButtonListeners();

        // Display version info
        displayVersionInfo();
    }

    private void initializeUIElements() {
        //load the termux activity automatically everytime the app launches
        autoStartOnBoot = findViewById(R.id.AutoStartOnBoot);

        //sets the port for the dashboard
        webPortBox = findViewById(R.id.WebPortBox);

        //sets the port for ssh connections
        sshPortBox = findViewById(R.id.SSHPortBox);

        //monitors the app every interval selected by the user (temperature and storage)
        spinnerMonitoringInterval = findViewById(R.id.spinnerMonitoringInterval);

        //warns the user when the phone gets to the temperature set by the user
        tempBox = findViewById(R.id.TempBox);

        //warns the user when the storage gets to the percentage set by the user
        lowStorageBox = findViewById(R.id.LowStorageBox);

        //button to reset all settings to default
        resetBtn = findViewById(R.id.ResetBtn);

        //button to save all settings
        saveBtn = findViewById(R.id.SaveBtn);

        //displays the current version of the app
        tvAppVersion2 = findViewById(R.id.tvAppVersion2);

        //displays the current version of busybox installed
        tvBusyBox2 = findViewById(R.id.tvBusyBox2);

        //button to open the system files configuration page
        systemFilesBtn = findViewById(R.id.SystemFilesBtn);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, monitoringIntervalOptions) {
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
        spinnerMonitoringInterval.setAdapter(adapter);
    }

    private void loadSettings() {
        // Load settings from ConfigManager
        autoStartOnBoot.setChecked(configManager.getAutoStart());
        webPortBox.setText(configManager.getWebPort());
        sshPortBox.setText(configManager.getSSHPort());
        tempBox.setText(configManager.getTempAlert());
        lowStorageBox.setText(configManager.getLowStorage());

        // Set spinner to saved monitoring interval
        String savedInterval = configManager.getMonitoringInterval();
        for (int i = 0; i < monitoringIntervalOptions.length; i++) {
            if (monitoringIntervalOptions[i].equals(savedInterval)) {
                spinnerMonitoringInterval.setSelection(i);
                break;
            }
        }
    }

    private void setupButtonListeners() {
        // Save button
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Reset button
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToDefaults();
            }
        });

        // System files button (placeholder - implement navigation as needed)
        systemFilesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement navigation to system files configuration page
                Toast.makeText(configuration_page.this, "System files configuration coming soon", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSettings() {
        try {
            // Get values from UI
            boolean autoStart = autoStartOnBoot.isChecked();
            String webPort = webPortBox.getText().toString().trim();
            String sshPort = sshPortBox.getText().toString().trim();
            String monitoringInterval = monitoringIntervalOptions[spinnerMonitoringInterval.getSelectedItemPosition()];
            String tempAlert = tempBox.getText().toString().trim();
            String lowStorage = lowStorageBox.getText().toString().trim();

            // Use defaults if fields are empty
            if (webPort.isEmpty()) webPort = ConfigManager.DEFAULT_WEB_PORT;
            if (sshPort.isEmpty()) sshPort = ConfigManager.DEFAULT_SSH_PORT;
            if (tempAlert.isEmpty()) tempAlert = ConfigManager.DEFAULT_TEMP_ALERT;
            if (lowStorage.isEmpty()) lowStorage = ConfigManager.DEFAULT_LOW_STORAGE;

            // Save all settings
            configManager.saveAllSettings(autoStart, webPort, sshPort, monitoringInterval, tempAlert, lowStorage);

            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToDefaults() {
        // Reset to defaults in ConfigManager
        configManager.resetToDefaults();

        // Reload settings from ConfigManager (which now has defaults)
        loadSettings();

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private void displayVersionInfo() {
        //displays the current version of the app
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvAppVersion2.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion2.setText("Unknown");
        }

        //displays the current version of busybox installed
        // TODO: Implement BusyBox version detection
        tvBusyBox2.setText("1.36.1"); // Placeholder - replace with actual version detection
    }
}
