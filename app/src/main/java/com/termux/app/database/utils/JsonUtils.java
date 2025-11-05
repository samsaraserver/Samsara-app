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
        if (user.getProfilePictureUrl() != null) {
            json.put("profile_picture_url", user.getProfilePictureUrl());
        }
        if (user.getIsActive() != null) {
            json.put("is_active", user.getIsActive());
        }
        // OAuth fields
        if (user.getGithubId() != null) {
            json.put("github_id", user.getGithubId());
        }
        if (user.getAuthProvider() != null) {
            json.put("auth_provider", user.getAuthProvider());
        }
        if (user.getOauthToken() != null) {
            json.put("oauth_token", user.getOauthToken());
        }

        return json;
    }
    
    public static SamsaraUser jsonToUser(JSONObject json) throws JSONException {
        SamsaraUser user = new SamsaraUser();

        user.setId(safeGetLong(json, "id", 0L));
        user.setUsername(safeGetString(json, "username", null));
        user.setEmail(safeGetString(json, "email", null));
        user.setPasswordHash(safeGetString(json, "password_hash", null));
        user.setProfilePictureUrl(safeGetString(json, "profile_picture_url", null));
        user.setIsActive(safeGetBoolean(json, "is_active", true));

        // OAuth fields
        user.setGithubId(safeGetString(json, "github_id", null));
        user.setAuthProvider(safeGetString(json, "auth_provider", null));
        user.setOauthToken(safeGetString(json, "oauth_token", null));

        String createdAtStr = safeGetString(json, "created_at", null);
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                String cleanTimestamp = createdAtStr.replace('T', ' ');
                if (cleanTimestamp.contains(".")) {
                    cleanTimestamp = cleanTimestamp.substring(0, cleanTimestamp.indexOf('.'));
                }
                if (cleanTimestamp.length() > 19) {
                    cleanTimestamp = cleanTimestamp.substring(0, 19);
                }
                user.setCreatedAt(Timestamp.valueOf(cleanTimestamp));
            } catch (Exception e) {
                user.setCreatedAt(null);
            }
        }

        String updatedAtStr = safeGetString(json, "updated_at", null);
        if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
            try {
                String cleanTimestamp = updatedAtStr.replace('T', ' ');
                if (cleanTimestamp.contains(".")) {
                    cleanTimestamp = cleanTimestamp.substring(0, cleanTimestamp.indexOf('.'));
                }
                if (cleanTimestamp.length() > 19) {
                    cleanTimestamp = cleanTimestamp.substring(0, 19);
                }
                user.setUpdatedAt(Timestamp.valueOf(cleanTimestamp));
            } catch (Exception e) {
                user.setUpdatedAt(null);
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
        
        long currentTime = System.currentTimeMillis();
        String timestamp = new java.sql.Timestamp(currentTime).toString();
        json.put("created_at", timestamp);
        json.put("updated_at", timestamp);
        
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