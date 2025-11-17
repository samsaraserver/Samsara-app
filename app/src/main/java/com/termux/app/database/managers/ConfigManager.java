package com.termux.app.database.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static final String PREFS_NAME = "samsara_config";

    // Keys for configuration settings
    private static final String KEY_AUTO_START = "auto_start_on_boot";
    private static final String KEY_WEB_PORT = "web_dashboard_port";
    private static final String KEY_SSH_PORT = "ssh_port";
    private static final String KEY_MONITORING_INTERVAL = "monitoring_interval";
    private static final String KEY_TEMP_ALERT = "temperature_alert";
    private static final String KEY_LOW_STORAGE = "low_storage_warning";

    // Default values
    public static final boolean DEFAULT_AUTO_START = false;
    public static final String DEFAULT_WEB_PORT = "8080";
    public static final String DEFAULT_SSH_PORT = "2222";
    public static final String DEFAULT_MONITORING_INTERVAL = "30s";
    public static final String DEFAULT_TEMP_ALERT = "60";
    public static final String DEFAULT_LOW_STORAGE = "8";

    private static ConfigManager instance;
    private SharedPreferences prefs;
    private final Context appContext;

    private ConfigManager(Context context) {
        this.appContext = context.getApplicationContext();
        prefs = this.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    // Getters
    public boolean getAutoStart() {
        return prefs.getBoolean(KEY_AUTO_START, DEFAULT_AUTO_START);
    }

    public String getWebPort() {
        return prefs.getString(KEY_WEB_PORT, DEFAULT_WEB_PORT);
    }

    public String getSSHPort() {
        return prefs.getString(KEY_SSH_PORT, DEFAULT_SSH_PORT);
    }

    public String getMonitoringInterval() {
        return prefs.getString(KEY_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL);
    }

    public String getTempAlert() {
        return prefs.getString(KEY_TEMP_ALERT, DEFAULT_TEMP_ALERT);
    }

    public String getLowStorage() {
        return prefs.getString(KEY_LOW_STORAGE, DEFAULT_LOW_STORAGE);
    }

    // Setters
    public void setAutoStart(boolean autoStart) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_START, autoStart);
        editor.apply();
        Log.d(TAG, "Auto start set to: " + autoStart);
    }

    public void setWebPort(String port) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_WEB_PORT, port);
        editor.apply();
        Log.d(TAG, "Web port set to: " + port);
    }

    public void setSSHPort(String port) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SSH_PORT, port);
        editor.apply();
        Log.d(TAG, "SSH port set to: " + port);
    }

    public void setMonitoringInterval(String interval) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_MONITORING_INTERVAL, interval);
        editor.apply();
        Log.d(TAG, "Monitoring interval set to: " + interval);
    }

    public void setTempAlert(String temp) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TEMP_ALERT, temp);
        editor.apply();
        Log.d(TAG, "Temperature alert set to: " + temp);
    }

    public void setLowStorage(String storage) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LOW_STORAGE, storage);
        editor.apply();
        Log.d(TAG, "Low storage warning set to: " + storage);
    }

    // Save all settings at once
    public void saveAllSettings(boolean autoStart, String webPort, String sshPort,
                                String monitoringInterval, String tempAlert, String lowStorage) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_START, autoStart);
        editor.putString(KEY_WEB_PORT, webPort);
        editor.putString(KEY_SSH_PORT, sshPort);
        editor.putString(KEY_MONITORING_INTERVAL, monitoringInterval);
        editor.putString(KEY_TEMP_ALERT, tempAlert);
        editor.putString(KEY_LOW_STORAGE, lowStorage);
        editor.apply();
        Log.d(TAG, "All settings saved");
    }

    // Reset all settings to defaults
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTO_START, DEFAULT_AUTO_START);
        editor.putString(KEY_WEB_PORT, DEFAULT_WEB_PORT);
        editor.putString(KEY_SSH_PORT, DEFAULT_SSH_PORT);
        editor.putString(KEY_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL);
        editor.putString(KEY_TEMP_ALERT, DEFAULT_TEMP_ALERT);
        editor.putString(KEY_LOW_STORAGE, DEFAULT_LOW_STORAGE);
        editor.apply();
        Log.d(TAG, "All settings reset to defaults");
    }
}