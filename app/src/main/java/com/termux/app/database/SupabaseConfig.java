package com.termux.app.database;

import android.content.Context;
import android.content.res.AssetManager;
import okhttp3.OkHttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class SupabaseConfig {
    private static String SUPABASE_URL;
    private static String SUPABASE_PUBLISHABLE_KEY;
    private static String REST_API_URL;
    private static OkHttpClient httpClient;
    private static boolean initialized = false;
    
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (SupabaseConfig.class) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();
                }
            }
        }
        return httpClient;
    }
    
    public static String getRestApiUrl() {
        return REST_API_URL;
    }
    
    public static String getApiKey() {
        return SUPABASE_PUBLISHABLE_KEY;
    }
    
    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }
    
    public static void initialize(Context context) throws Exception {
        if (initialized) {
            return;
        }
        
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("supabase.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            
            SUPABASE_URL = properties.getProperty("SUPABASE_URL");
            SUPABASE_PUBLISHABLE_KEY = properties.getProperty("SUPABASE_PUBLISHABLE_KEY");
            
            if (SUPABASE_URL == null || SUPABASE_URL.isEmpty()) {
                throw new IllegalStateException("Supabase URL is not configured");
            }
            if (SUPABASE_PUBLISHABLE_KEY == null || SUPABASE_PUBLISHABLE_KEY.isEmpty()) {
                throw new IllegalStateException("Supabase API key is not configured");
            }
            
            REST_API_URL = SUPABASE_URL + "/rest/v1/";
            
            OkHttpClient client = getHttpClient();
            if (client == null) {
                throw new IllegalStateException("Failed to initialize HTTP client");
            }
            
            initialized = true;
            
        } catch (IOException e) {
            throw new Exception("Failed to load Supabase configuration: " + e.getMessage(), e);
        }
    }
    
    public static void initialize() throws Exception {
        throw new IllegalStateException("Context required for initialization. Use initialize(Context) instead.");
    }
}