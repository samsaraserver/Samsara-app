package com.termux.app.database;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class SupabaseConfig {
    private static final String SUPABASE_URL = "https://myuhuqcbmvtwpnymcypz.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY = "sb_publishable_XJ_SWWZDmzB0MQm0e5h9eA_yvKBreag";
    
    // Note: Service role key should NEVER be used in mobile apps for security reasons
    
    private static final String REST_API_URL = SUPABASE_URL + "/rest/v1/";
    
    private static OkHttpClient httpClient;
    
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
    
    // Removed service role key method - not safe for mobile apps
    
    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }
    
    public static void initialize() throws Exception {
        if (SUPABASE_URL == null || SUPABASE_URL.isEmpty()) {
            throw new IllegalStateException("Supabase URL is not configured");
        }
        if (SUPABASE_PUBLISHABLE_KEY == null || SUPABASE_PUBLISHABLE_KEY.isEmpty()) {
            throw new IllegalStateException("Supabase API key is not configured");
        }
        
        OkHttpClient client = getHttpClient();
        if (client == null) {
            throw new IllegalStateException("Failed to initialize HTTP client");
        }
    }
}