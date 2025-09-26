package com.termux.app.database.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.termux.app.database.models.SamsaraUser;
import java.sql.Timestamp;

public class JsonUtils {
    
    public static JSONObject userToJson(SamsaraUser user) throws JSONException {
        JSONObject json = new JSONObject();
        
        if (user.getUsername() != null) {
            json.put("username", user.getUsername());
        }
        if (user.getEmail() != null) {
            json.put("email", user.getEmail());
        }
        if (user.getPasswordHash() != null) {
            json.put("password_hash", user.getPasswordHash());
        }
        if (user.getIsActive() != null) {
            json.put("is_active", user.getIsActive());
        }
        
        return json;
    }
    
    public static SamsaraUser jsonToUser(JSONObject json) throws JSONException {
        SamsaraUser user = new SamsaraUser();
        
        user.setId(safeGetLong(json, "id", 0L));
        user.setUsername(safeGetString(json, "username", null));
        user.setEmail(safeGetString(json, "email", null));
        user.setPasswordHash(safeGetString(json, "password_hash", null));
        user.setIsActive(safeGetBoolean(json, "is_active", true));
        
        String createdAtStr = safeGetString(json, "created_at", null);
        if (createdAtStr != null) {
            try {
                user.setCreatedAt(Timestamp.valueOf(createdAtStr.replace('T', ' ').substring(0, 19)));
            } catch (Exception e) {
                // If timestamp parsing fails, use current time
                user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            }
        }
        
        String updatedAtStr = safeGetString(json, "updated_at", null);
        if (updatedAtStr != null) {
            try {
                user.setUpdatedAt(Timestamp.valueOf(updatedAtStr.replace('T', ' ').substring(0, 19)));
            } catch (Exception e) {
                // If timestamp parsing fails, use current time
                user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            }
        }
        
        return user;
    }
    
    public static String createInsertPayload(String username, String email, String passwordHash) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("email", email);
        json.put("password_hash", passwordHash);
        json.put("is_active", true);
        
        return json.toString();
    }
    
    public static boolean isValidJsonResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Try to parse as JSONArray first (typical Supabase response)
            new JSONArray(responseBody);
            return true;
        } catch (JSONException e1) {
            try {
                // Try to parse as JSONObject
                new JSONObject(responseBody);
                return true;
            } catch (JSONException e2) {
                return false;
            }
        }
    }
    
    public static JSONObject safeGetJsonObject(JSONObject json, String key, JSONObject defaultValue) {
        try {
            return json.has(key) ? json.getJSONObject(key) : defaultValue;
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public static String safeGetString(JSONObject json, String key, String defaultValue) {
        try {
            return json.has(key) ? json.getString(key) : defaultValue;
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public static long safeGetLong(JSONObject json, String key, long defaultValue) {
        try {
            return json.has(key) ? json.getLong(key) : defaultValue;
        } catch (JSONException e) {
            return defaultValue;
        }
    }
    
    public static boolean safeGetBoolean(JSONObject json, String key, boolean defaultValue) {
        try {
            return json.has(key) ? json.getBoolean(key) : defaultValue;
        } catch (JSONException e) {
            return defaultValue;
        }
    }
}