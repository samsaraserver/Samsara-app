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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;

import com.termux.R;
import com.termux.app.config.SamsaraConfigManager;

public class configuration_page extends AppCompatActivity {
    private static final String TAG = "ConfigPage";
    private static final int IMPORT_REQUEST_CODE = 0x1234;

    private SwitchCompat autoStartOnBoot;
    private EditText webPortBox;
    private EditText sshPortBox;
    private Spinner spinnerMonitoringInterval;
    private EditText tempBox;
    private EditText lowStorageBox;
    private ImageButton ExportBtn;
    private ImageButton ImportBtn;
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
        ExportBtn = findViewById(R.id.ExportBtn);
        ImportBtn = findViewById(R.id.ImportBtn);
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
                    spinnerMonitoringInterval.setSelection(1);
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

        if (ExportBtn != null) {
            ExportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String exportJson = configManager.exportConfiguration();
                    if (exportJson == null) {
                        Toast.makeText(configuration_page.this, "Failed to generate export data", Toast.LENGTH_LONG).show();
                        return;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                File exportsDir = getExternalFilesDir("exports");
                                if (exportsDir == null) exportsDir = getFilesDir();
                                if (!exportsDir.exists()) exportsDir.mkdirs();

                                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                                String filename = "samsara_config_export_" + timestamp + ".json";
                                final File outFile = new File(exportsDir, filename);

                                FileOutputStream fos = new FileOutputStream(outFile);
                                try {
                                    fos.write(exportJson.getBytes("UTF-8"));
                                    fos.flush();
                                    fos.getFD().sync();
                                } finally {
                                    try { fos.close(); } catch (IOException ignored) {}
                                }

                                final Uri uri = FileProvider.getUriForFile(configuration_page.this, "com.termux.fileprovider", outFile);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(configuration_page.this, "Exported to: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

                                        Intent share = new Intent(Intent.ACTION_SEND);
                                        share.setType("application/json");
                                        share.putExtra(Intent.EXTRA_STREAM, uri);
                                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(Intent.createChooser(share, "Share exported config"));
                                    }
                                });
                            } catch (final Exception e) {
                                Log.e(TAG, "Export failed", e);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(configuration_page.this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });
        }

        if (ImportBtn != null) {
            ImportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    // allow any file as fallback
                    Intent chooser = Intent.createChooser(intent, "Select configuration JSON to import");
                    startActivityForResult(chooser, IMPORT_REQUEST_CODE);
                }
            });
        }

        if (systemFilesBtn != null) {
            systemFilesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            File dataDir = new File("/data");
                            final StringBuilder sb = new StringBuilder();

                            if (!dataDir.exists()) {
                                sb.append("/data does not exist or is not accessible on this device.");
                            } else {
                                File[] files = dataDir.listFiles();
                                if (files == null) {
                                    sb.append("Unable to list /data.");
                                } else if (files.length == 0) {
                                    sb.append("/data is empty");
                                } else {
                                    for (File f : files) {
                                        if (f.isDirectory()) sb.append("[D] "); else sb.append("[F] ");
                                        sb.append(f.getName()).append('\n');
                                    }
                                }
                            }

                            final String message = sb.toString();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(configuration_page.this)
                                        .setTitle("/data filesystem")
                                        .setMessage(message)
                                        .setPositiveButton("OK", null)
                                        .show();
                                }
                            });
                        }
                    }).start();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                final Uri uri = data.getData();
                if (uri == null) {
                    Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            if (is == null) throw new IOException("Unable to open selected file");
                            byte[] buf = new byte[is.available() > 0 ? is.available() : 4096];
                            int read;
                            StringBuilder sb = new StringBuilder();
                            byte[] tmp = new byte[4096];
                            while ((read = is.read(tmp)) != -1) {
                                sb.append(new String(tmp, 0, read, "UTF-8"));
                            }
                            final String json = sb.toString();

                            final boolean ok = configManager.importConfiguration(json);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (ok) {
                                        loadSettings();
                                        Toast.makeText(configuration_page.this, "Configuration imported", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(configuration_page.this, "Failed to import configuration", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } catch (final Exception e) {
                            Log.e(TAG, "Import failed", e);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(configuration_page.this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }).start();
            } else {
                Toast.makeText(this, "Import cancelled", Toast.LENGTH_SHORT).show();
            }
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
