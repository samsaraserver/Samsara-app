package com.termux.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import com.termux.app.config.SamsaraConfigManager;

public class configuration_page extends AppCompatActivity {
    private static final String TAG = "ConfigPage";

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
    private SamsaraConfigManager configManager;
    private String[] monitoringIntervalOptions = {"15s", "30s", "1m", "5m", "10m", "30m", "1h"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration_page);

        NavbarHelper.setupNavbar(this);

        try {
            configManager = new SamsaraConfigManager(this);
            Log.d(TAG, "Config file path: " + configManager.getConfigFilePath());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load config: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        initializeUIElements();
        setupSpinner();

        try {
            loadSettings();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        setupButtonListeners();
        displayVersionInfo();
    }

    private void initializeUIElements() {
        autoStartOnBoot = findViewById(R.id.AutoStartOnBoot);
        webPortBox = findViewById(R.id.WebPortBox);
        sshPortBox = findViewById(R.id.SSHPortBox);
        spinnerMonitoringInterval = findViewById(R.id.spinnerMonitoringInterval);
        tempBox = findViewById(R.id.TempBox);
        lowStorageBox = findViewById(R.id.LowStorageBox);
        resetBtn = findViewById(R.id.ResetBtn);
        saveBtn = findViewById(R.id.SaveBtn);
        tvAppVersion2 = findViewById(R.id.tvAppVersion2);
        tvBusyBox2 = findViewById(R.id.tvBusyBox2);
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

        spinnerMonitoringInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Monitoring interval spinner selected: " + monitoringIntervalOptions[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Monitoring interval spinner: nothing selected");
            }
        });
    }

    private void loadSettings() {
        Log.d(TAG, "Loading settings from config...");

        boolean autoStart = configManager.isAutoStartOnBoot();
        String webPort = configManager.getWebDashboardPort();
        String sshPort = configManager.getSshPort();
        String tempAlert = configManager.getTemperatureAlert();
        String lowStorage = configManager.getLowStorageWarningThreshold();
        String savedInterval = configManager.getMonitoringInterval();

        Log.d(TAG, "Loaded: autoStart=" + autoStart + ", webPort=" + webPort + ", sshPort=" + sshPort + ", interval=" + savedInterval);

        if (autoStartOnBoot != null) autoStartOnBoot.setChecked(autoStart);
        if (webPortBox != null) webPortBox.setText(webPort);
        if (sshPortBox != null) sshPortBox.setText(sshPort);
        if (tempBox != null) tempBox.setText(tempAlert);
        if (lowStorageBox != null) lowStorageBox.setText(lowStorage);

        // Set spinner selection safely
        if (spinnerMonitoringInterval != null && savedInterval != null) {
            int sel = -1;
            for (int i = 0; i < monitoringIntervalOptions.length; i++) {
                if (monitoringIntervalOptions[i].equals(savedInterval)) {
                    sel = i;
                    break;
                }
            }
            if (sel >= 0) {
                spinnerMonitoringInterval.setSelection(sel);
                Log.d(TAG, "Set interval spinner to position " + sel + ": " + savedInterval);
            } else {
                // try tolerant match (number only)
                String numberPart = savedInterval.replaceAll("[^0-9]", "");
                if (!numberPart.isEmpty()) {
                    String tolerant = numberPart + (savedInterval.toLowerCase().contains("m") ? "m" : "s");
                    for (int i = 0; i < monitoringIntervalOptions.length; i++) {
                        if (monitoringIntervalOptions[i].equals(tolerant)) {
                            spinnerMonitoringInterval.setSelection(i);
                            sel = i;
                            break;
                        }
                    }
                }
                if (sel < 0) {
                    spinnerMonitoringInterval.setSelection(1); // default 30s
                    Log.d(TAG, "Saved interval not found, defaulting spinner to 30s");
                }
            }
        }
    }

    private void setupButtonListeners() {
        if (saveBtn != null) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveSettings();
                }
            });
        }

        if (resetBtn != null) {
            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetToDefaults();
                }
            });
        }

        if (systemFilesBtn != null) {
            systemFilesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(configuration_page.this, "System files configuration coming soon", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveSettings() {
        Log.d(TAG, "Save button clicked");
        try {
            boolean autoStart = (autoStartOnBoot != null) && autoStartOnBoot.isChecked();
            String webPort = webPortBox != null ? webPortBox.getText().toString().trim() : "8080";
            String sshPort = sshPortBox != null ? sshPortBox.getText().toString().trim() : "2222";
            int selIndex = (spinnerMonitoringInterval != null) ? spinnerMonitoringInterval.getSelectedItemPosition() : 1;
            if (selIndex < 0 || selIndex >= monitoringIntervalOptions.length) selIndex = 1;
            String monitoringInterval = monitoringIntervalOptions[selIndex];
            String tempAlert = tempBox != null ? tempBox.getText().toString().trim() : "60";
            String lowStorage = lowStorageBox != null ? lowStorageBox.getText().toString().trim() : "8";

            Log.d(TAG, "Saving: autoStart=" + autoStart + ", webPort=" + webPort + ", sshPort=" + sshPort + ", interval=" + monitoringInterval);

            // Use defaults if fields are empty
            if (webPort.isEmpty()) webPort = "8080";
            if (sshPort.isEmpty()) sshPort = "2222";
            if (tempAlert.isEmpty()) tempAlert = "60";
            if (lowStorage.isEmpty()) lowStorage = "8";

            configManager.setAutoStartOnBoot(autoStart);
            configManager.setWebDashboardPort(webPort);
            configManager.setSshPort(sshPort);
            configManager.setMonitoringInterval(monitoringInterval);
            configManager.setTemperatureAlert(tempAlert);
            configManager.setLowStorageWarningThreshold(lowStorage);

            boolean saved = configManager.saveConfiguration();
            Log.d(TAG, "Save configuration result: " + saved);

            if (saved) {
                Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save settings", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings", e);
            Toast.makeText(this, "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToDefaults() {
        boolean ok = configManager.resetToDefaults();
        if (!ok) {
            Toast.makeText(this, "Failed to reset to defaults", Toast.LENGTH_LONG).show();
            return;
        }

        loadSettings();

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private void displayVersionInfo() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (tvAppVersion2 != null) tvAppVersion2.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            if (tvAppVersion2 != null) tvAppVersion2.setText("Unknown");
        }

        if (tvBusyBox2 != null) tvBusyBox2.setText("1.36.1");
    }
}
