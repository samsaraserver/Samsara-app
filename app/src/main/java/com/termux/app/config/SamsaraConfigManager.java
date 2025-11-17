package com.termux.app.config;

import android.content.Context;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SamsaraConfigManager {
    
    private static final String TAG = "SamsaraConfigManager";
    private static final String CONFIG_FILE_NAME = "samsara_config.json";
    private static final String CONFIG_ASSETS_PATH = "settings/" + CONFIG_FILE_NAME;
    
    private Context context;
    private JSONObject configData;
    private File configFile;

    public SamsaraConfigManager(Context context) {
        this.context = context;
        this.configFile = new File(context.getFilesDir(), CONFIG_FILE_NAME);
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        try {
            ensureConfigFileExists();

            String jsonString = readFromFile(configFile);
            configData = new JSONObject(jsonString);
        } catch (JSONException | IOException e) {
            Log.w(TAG, "Failed to load configuration from file, loading defaults: " + e.getMessage(), e);
            loadDefaultConfiguration();
            saveConfiguration();
        }
    }

    private void ensureConfigFileExists() throws IOException {
        if (configFile.exists()) return;

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = context.getAssets().open(CONFIG_ASSETS_PATH);
            outputStream = new FileOutputStream(configFile);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.getFD().sync();
        } finally {
            if (inputStream != null) try { inputStream.close(); } catch (IOException ignored) {}
            if (outputStream != null) try { outputStream.close(); } catch (IOException ignored) {}
        }
    }
    
    private String readFromAssets() throws IOException {
        InputStream inputStream = context.getAssets().open(CONFIG_ASSETS_PATH);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
    
    private String readFromFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        int read = 0;
        int offset = 0;
        while ((read = inputStream.read(buffer, offset, size - offset)) > 0) {
            offset += read;
            if (offset >= size) break;
        }
        inputStream.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
    
    private void loadDefaultConfiguration() {
        try {
            String defaultConfig = readFromAssets();
            configData = new JSONObject(defaultConfig);
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Failed to load default configuration from assets: " + e.getMessage(), e);
            createEmptyConfiguration();
        }
    }
    
    private void createEmptyConfiguration() {
        configData = new JSONObject();
        try {
            configData.put("version", "1.0.0");
            updateLastModified();
            
            JSONObject general = new JSONObject();
            general.put("autoStartOnBoot", false);
            configData.put("general", general);
            
            JSONObject network = new JSONObject();
            network.put("dashboardPassword", "server");
            network.put("webDashboardPort", "8080");
            network.put("sshPort", "2222");
            configData.put("network", network);
            
            JSONObject monitoring = new JSONObject();
            monitoring.put("monitoringInterval", "30s");
            monitoring.put("temperatureAlert", "60");
            configData.put("monitoring", monitoring);
            
            JSONObject communityProjects = new JSONObject();
            communityProjects.put("autoUpdateProject", false);
            communityProjects.put("installationDirectory", "/data");
            configData.put("communityProjects", communityProjects);
            
            JSONObject advanced = new JSONObject();
            advanced.put("lowStorageWarningThreshold", "8");
            configData.put("advanced", advanced);
            
        } catch (JSONException e) {
            configData = new JSONObject();
            try {
                configData.put("version", "1.0.0");
                configData.put("general", new JSONObject());
                configData.put("network", new JSONObject());
                configData.put("monitoring", new JSONObject());
                configData.put("communityProjects", new JSONObject());
                configData.put("advanced", new JSONObject());
            } catch (JSONException fallbackException) {
                configData = new JSONObject();
            }
        }
    }
    
    public boolean saveConfiguration() {
        try {
            updateLastModified();


            if (!configFile.exists()) {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                configFile.createNewFile();
            }

            FileOutputStream outputStream = new FileOutputStream(configFile, false);
            try {
                byte[] data = configData.toString(4).getBytes(StandardCharsets.UTF_8);
                outputStream.write(data);
                outputStream.flush();
                outputStream.getFD().sync();
            } finally {
                outputStream.close();
            }

            Log.d(TAG, "Configuration saved to: " + configFile.getAbsolutePath());
            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to save configuration: " + e.getMessage(), e);
            return false;
        }
    }
    
    private void updateLastModified() throws JSONException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        configData.put("lastModified", dateFormat.format(new Date()));
    }
    
    public String getConfigFilePath() {
        return configFile.getAbsolutePath();
    }

    public boolean getBoolean(String section, String key, boolean defaultValue) {
        try {
            return configData.getJSONObject(section).getBoolean(key);
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public String getString(String section, String key, String defaultValue) {
        try {
            return configData.getJSONObject(section).getString(key);
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public int getInt(String section, String key, int defaultValue) {
        try {
            return configData.getJSONObject(section).getInt(key);
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public boolean setBoolean(String section, String key, boolean value) {
        try {
            if (!configData.has(section)) {
                configData.put(section, new JSONObject());
            }
            configData.getJSONObject(section).put(key, value);
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to set boolean config: " + section + "." + key + " = " + value, e);
            return false;
        }
    }
    
    public boolean setString(String section, String key, String value) {
        try {
            if (!configData.has(section)) {
                configData.put(section, new JSONObject());
            }
            configData.getJSONObject(section).put(key, value);
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to set string config: " + section + "." + key + " = " + value, e);
            return false;
        }
    }
    
    public boolean setInt(String section, String key, int value) {
        try {
            if (!configData.has(section)) {
                configData.put(section, new JSONObject());
            }
            configData.getJSONObject(section).put(key, value);
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to set int config: " + section + "." + key + " = " + value, e);
            return false;
        }
    }
    
    public boolean resetToDefaults() {
        try {
            String defaultConfig = readFromAssets();
            configData = new JSONObject(defaultConfig);
            return saveConfiguration();
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to reset to defaults: " + e.getMessage(), e);
            return false;
        }
    }
    
    public String exportConfiguration() {
        try {
            return configData.toString(4);
        } catch (JSONException e) {
            return null;
        }
    }
    
    public boolean importConfiguration(String jsonString) {
        try {
            JSONObject newConfig = new JSONObject(jsonString);
            configData = newConfig;
            return saveConfiguration();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to import configuration: " + e.getMessage(), e);
            return false;
        }
    }
    
    public boolean isAutoStartOnBoot() {
        return getBoolean("general", "autoStartOnBoot", false);
    }
    
    public boolean setAutoStartOnBoot(boolean enabled) {
        return setBoolean("general", "autoStartOnBoot", enabled);
    }
    
    public String getDashboardPassword() {
        return getString("network", "dashboardPassword", "server");
    }
    
    public boolean setDashboardPassword(String password) {
        return setString("network", "dashboardPassword", password);
    }
    
    public String getWebDashboardPort() {
        return getString("network", "webDashboardPort", "8080");
    }
    
    public boolean setWebDashboardPort(String port) {
        return setString("network", "webDashboardPort", port);
    }
    
    public String getSshPort() {
        return getString("network", "sshPort", "2222");
    }
    
    public boolean setSshPort(String port) {
        return setString("network", "sshPort", port);
    }
    
    public String getMonitoringInterval() {
        return getString("monitoring", "monitoringInterval", "30s");
    }
    
    public boolean setMonitoringInterval(String interval) {
        return setString("monitoring", "monitoringInterval", interval);
    }

    public long getMonitoringIntervalInMillis() {
        String interval = getMonitoringInterval();
        return parseIntervalToMillis(interval);
    }

    private long parseIntervalToMillis(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 30000;
        }

        try {
            String numberPart = interval.replaceAll("[^0-9]", "");
            String unitPart = interval.replaceAll("[0-9]", "").trim();

            if (numberPart.isEmpty()) return 30000;

            long value = Long.parseLong(numberPart);

            switch (unitPart.toLowerCase()) {
                case "s":
                case "sec":
                case "secs":
                    return value * 1000;
                case "m":
                case "min":
                case "mins":
                    return value * 60 * 1000;
                case "h":
                case "hr":
                case "hrs":
                    return value * 60 * 60 * 1000;
                default:
                    if (unitPart.isEmpty()) return value * 1000;
                    Log.w(TAG, "Unknown time unit: " + unitPart + ", defaulting to seconds");
                    return value * 1000;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse interval: " + interval, e);
            return 30000;
        }
    }

    public String getTemperatureAlert() {
        return getString("monitoring", "temperatureAlert", "60");
    }
    
    public boolean setTemperatureAlert(String temperature) {
        return setString("monitoring", "temperatureAlert", temperature);
    }
    
    public boolean isAutoUpdateProject() {
        return getBoolean("communityProjects", "autoUpdateProject", false);
    }
    
    public boolean setAutoUpdateProject(boolean enabled) {
        return setBoolean("communityProjects", "autoUpdateProject", enabled);
    }
    
    public String getInstallationDirectory() {
        return getString("communityProjects", "installationDirectory", "/data");
    }
    
    public boolean setInstallationDirectory(String directory) {
        return setString("communityProjects", "installationDirectory", directory);
    }
    
    public String getLowStorageWarningThreshold() {
        return getString("advanced", "lowStorageWarningThreshold", "8");
    }
    
    public boolean setLowStorageWarningThreshold(String threshold) {
        return setString("advanced", "lowStorageWarningThreshold", threshold);
    }
}
