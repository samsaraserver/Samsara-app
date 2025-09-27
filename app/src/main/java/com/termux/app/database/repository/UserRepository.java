package com.termux.app.database.repository;

import android.util.Log;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.models.SamsaraUser;
import com.termux.app.database.utils.JsonUtils;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String TABLE_NAME = "samsara_user";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final String baseUrl;
    private final String apiKey;
    
    public UserRepository() {
        this.httpClient = SupabaseConfig.getHttpClient();
        this.executorService = Executors.newCachedThreadPool();
        this.baseUrl = SupabaseConfig.getRestApiUrl();
        this.apiKey = SupabaseConfig.getApiKey();
    }
    
    public CompletableFuture<Boolean> createUser(String username, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String passwordHash = hashPassword(password);
                String jsonPayload = JsonUtils.createInsertPayload(username, email, passwordHash);
                
                RequestBody body = RequestBody.create(jsonPayload, JSON);
                Request request = new Request.Builder()
                    .url(baseUrl + TABLE_NAME)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "User created successfully with status: " + response.code());
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to create user. Status: " + response.code() + ", Error: " + errorBody);
                        return false;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating user: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }
    
    public CompletableFuture<SamsaraUser> getUserByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + TABLE_NAME + "?email=eq." + email + "&select=*";
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        
                        if (jsonArray.length() > 0) {
                            JSONObject userJson = jsonArray.getJSONObject(0);
                            return JsonUtils.jsonToUser(userJson);
                        }
                    } else {
                        Log.e(TAG, "Failed to get user by email. Status: " + response.code());
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting user by email: " + e.getMessage(), e);
            }
            return null;
        }, executorService);
    }
    
    public CompletableFuture<SamsaraUser> getUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + TABLE_NAME + "?username=eq." + username + "&select=*";
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        
                        if (jsonArray.length() > 0) {
                            JSONObject userJson = jsonArray.getJSONObject(0);
                            return JsonUtils.jsonToUser(userJson);
                        }
                    } else {
                        Log.e(TAG, "Failed to get user by username. Status: " + response.code());
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting user by username: " + e.getMessage(), e);
            }
            return null;
        }, executorService);
    }
    
    public CompletableFuture<Boolean> authenticateUser(String emailOrUsername, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, get user by email or username
                SamsaraUser user = null;
                
                // Try email first
                if (emailOrUsername.contains("@")) {
                    user = getUserByEmail(emailOrUsername).get();
                } else {
                    user = getUserByUsername(emailOrUsername).get();
                }
                
                if (user != null && user.getPasswordHash() != null) {
                    return verifyPassword(password, user.getPasswordHash());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error authenticating user: " + e.getMessage(), e);
            }
            return false;
        }, executorService);
    }
    
    public CompletableFuture<Boolean> checkUsernameExists(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SamsaraUser user = getUserByUsername(username).get();
                return user != null;
            } catch (Exception e) {
                Log.e(TAG, "Error checking username existence: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }
    
    public CompletableFuture<Boolean> checkEmailExists(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SamsaraUser user = getUserByEmail(email).get();
                return user != null;
            } catch (Exception e) {
                Log.e(TAG, "Error checking email existence: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }
    
    private String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage(), e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    private boolean verifyPassword(String password, String storedHash) {
        try {
            byte[] combined = Base64.getDecoder().decode(storedHash);
            
            // Extract salt (first 16 bytes)
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);
            
            // Hash the provided password with the extracted salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Compare with stored hash (remaining bytes)
            for (int i = 0; i < hashedPassword.length; i++) {
                if (hashedPassword[i] != combined[16 + i]) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password: " + e.getMessage(), e);
            return false;
        }
    }
    
    public CompletableFuture<Boolean> updateUser(SamsaraUser user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject updateData = new JSONObject();
                if (user.getUsername() != null) {
                    updateData.put("username", user.getUsername());
                }
                if (user.getEmail() != null) {
                    updateData.put("email", user.getEmail());
                }
                if (user.getPasswordHash() != null) {
                    updateData.put("password_hash", user.getPasswordHash());
                }
                if (user.getIsActive() != null) {
                    updateData.put("is_active", user.getIsActive());
                }
                
                String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
                updateData.put("updated_at", timestamp);
                
                RequestBody body = RequestBody.create(updateData.toString(), JSON);
                String url = baseUrl + TABLE_NAME + "?id=eq." + user.getId();
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "User updated successfully with status: " + response.code());
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to update user. Status: " + response.code() + ", Error: " + errorBody);
                        return false;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating user: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }

    public CompletableFuture<Boolean> updateUserInfo(Long userId, String username, String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("username", username);
                updateData.put("email", email);
                
                String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
                updateData.put("updated_at", timestamp);
                
                RequestBody body = RequestBody.create(updateData.toString(), JSON);
                String url = baseUrl + TABLE_NAME + "?id=eq." + userId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "User info updated successfully");
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to update user info. Status: " + response.code() + ", Error: " + errorBody);
                        return false;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating user info: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }

    public CompletableFuture<Boolean> updateUserWithPassword(Long userId, String username, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String passwordHash = hashPassword(password);
                
                JSONObject updateData = new JSONObject();
                updateData.put("username", username);
                updateData.put("email", email);
                updateData.put("password_hash", passwordHash);
                
                String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
                updateData.put("updated_at", timestamp);
                
                RequestBody body = RequestBody.create(updateData.toString(), JSON);
                String url = baseUrl + TABLE_NAME + "?id=eq." + userId;
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "User info with password updated successfully");
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to update user with password. Status: " + response.code() + ", Error: " + errorBody);
                        return false;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating user with password: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + TABLE_NAME + "?select=count&limit=0";
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    boolean isConnected = response.isSuccessful();
                    if (!isConnected) {
                        Log.e(TAG, "Connection test failed. Status: " + response.code());
                        if (response.body() != null) {
                            Log.e(TAG, "Error response: " + response.body().string());
                        }
                    }
                    return isConnected;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Connection test error: " + e.getMessage(), e);
                return false;
            }
        }, executorService);
    }
    
    private boolean isValidResponse(Response response) {
        if (!response.isSuccessful()) {
            Log.w(TAG, "API request failed with status: " + response.code());
            return false;
        }
        
        Log.d(TAG, "Response successful with status: " + response.code());
        return true;
    }
}