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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static UserRepository instance;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final String baseUrl;
    private final String apiKey;

    private UserRepository() {
        this.httpClient = SupabaseConfig.getHttpClient();
        this.executorService = Executors.newCachedThreadPool();
        this.baseUrl = SupabaseConfig.getRestApiUrl();
        this.apiKey = SupabaseConfig.getApiKey();
    }

    public static synchronized UserRepository getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
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
                String url = baseUrl + TABLE_NAME + "?" + buildIlikeFilter("email", email) + "&select=*";
                
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
                String url = baseUrl + TABLE_NAME + "?" + buildIlikeFilter("username", username) + "&select=*";
                
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
                SamsaraUser user = null;
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
    
    private String buildIlikeFilter(String column, String value) {
        if (value == null) {
            return column + "=ilike.";
        }
        try {
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            return column + "=ilike." + encodedValue;
        } catch (Exception e) {
            Log.e(TAG, "Failed to encode filter value", e);
            return column + "=ilike." + value;
        }
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
            
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
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
    
    public CompletableFuture<SamsaraUser> updateUser(SamsaraUser user) {
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
                if (user.getProfilePictureUrl() != null) {
                    updateData.put("profile_picture_url", user.getProfilePictureUrl());
                }
                // OAuth fields
                if (user.getGithubId() != null) {
                    updateData.put("github_id", user.getGithubId());
                }
                if (user.getAuthProvider() != null) {
                    updateData.put("auth_provider", user.getAuthProvider());
                }
                if (user.getOauthToken() != null) {
                    updateData.put("oauth_token", user.getOauthToken());
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
                    .addHeader("Prefer", "return=representation")
                    .patch(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        if (jsonArray.length() > 0) {
                            return JsonUtils.jsonToUser(jsonArray.getJSONObject(0));
                        }
                        return user;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to update user. Status: " + response.code() + ", Error: " + errorBody);
                        return null;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error updating user: " + e.getMessage(), e);
                return null;
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
    
    public CompletableFuture<String> uploadProfilePicture(Long userId, byte[] imageData) {
        return CompletableFuture.supplyAsync(() -> {
            if (userId == null || userId <= 0 || imageData == null || imageData.length == 0) {
                Log.e(TAG, "Invalid upload parameters");
                return null;
            }
            if (imageData.length > 5 * 1024 * 1024) {
                Log.e(TAG, "Image exceeds 5MB limit");
                return null;
            }
            
            String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
            String uploadUrl = SupabaseConfig.getSupabaseUrl() + "/storage/v1/object/samsara_profile_pictures/" + filename;
            
            RequestBody fileBody = RequestBody.create(imageData, MediaType.get("image/jpeg"));
            Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .post(fileBody)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return filename;
                } else {
                    Log.e(TAG, "Upload failed with status: " + response.code());
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload exception: " + e.getMessage());
                return null;
            }
        }, executorService);
    }
    

    
    public CompletableFuture<Boolean> updateUserProfilePicture(Long userId, String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (userId == null || userId <= 0) return false;
                if (filename == null || filename.isEmpty()) return false;
                if (!filename.matches("\\d+_\\d+\\.jpg")) {
                    Log.e(TAG, "Invalid profile picture filename format: " + filename);
                    return false;
                }
                JSONObject updateData = new JSONObject();
                updateData.put("profile_picture_url", filename);
                updateData.put("updated_at", new java.sql.Timestamp(System.currentTimeMillis()).toString());
                
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
                        return true;
                    } else {
                        Log.e(TAG, "Profile picture update failed with status: " + response.code());
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating profile picture: " + e.getMessage());
                return false;
            }
        }, executorService);
    }
    
    public String getProfilePictureUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        return SupabaseConfig.getSupabaseUrl() + "/storage/v1/object/public/samsara_profile_pictures/" + filename;
    }
    
    private boolean isValidResponse(Response response) {
        if (!response.isSuccessful()) {
            Log.e(TAG, "API request failed with status: " + response.code());
            return false;
        }
        return true;
    }

    // OAuth-specific methods

    /**
     * Find user by GitHub ID
     */
    public CompletableFuture<SamsaraUser> findByGithubId(String githubId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + TABLE_NAME + "?github_id=eq." + URLEncoder.encode(githubId, StandardCharsets.UTF_8.name()) + "&select=*";

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
                        Log.e(TAG, "Failed to get user by GitHub ID. Status: " + response.code());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting user by GitHub ID: " + e.getMessage(), e);
            }
            return null;
        }, executorService);
    }

    /**
     * Find user by ID
     */
    public CompletableFuture<SamsaraUser> findById(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = baseUrl + TABLE_NAME + "?id=eq." + userId + "&select=*";

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
                        Log.e(TAG, "Failed to get user by ID. Status: " + response.code());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting user by ID: " + e.getMessage(), e);
            }
            return null;
        }, executorService);
    }

    /**
     * Alias for getUserByEmail (for OAuth compatibility)
     */
    public CompletableFuture<SamsaraUser> findByEmail(String email) {
        return getUserByEmail(email);
    }

    /**
     * Check if username is available
     */
    public CompletableFuture<Boolean> isUsernameAvailable(String username) {
        return checkUsernameExists(username).thenApply(exists -> !exists);
    }

    /**
     * Check if email is available
     */
    public CompletableFuture<Boolean> isEmailAvailable(String email) {
        return checkEmailExists(email).thenApply(exists -> !exists);
    }

    /**
     * Create user from SamsaraUser object (for OAuth users)
     */
    public CompletableFuture<SamsaraUser> createUser(SamsaraUser user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject userData = new JSONObject();
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("password_hash", user.getPasswordHash());
                userData.put("profile_picture_url", user.getProfilePictureUrl());
                userData.put("is_active", user.getIsActive() != null ? user.getIsActive() : true);

                // OAuth fields
                if (user.getGithubId() != null) {
                    userData.put("github_id", user.getGithubId());
                }
                if (user.getAuthProvider() != null) {
                    userData.put("auth_provider", user.getAuthProvider());
                } else {
                    userData.put("auth_provider", "email");
                }
                if (user.getOauthToken() != null) {
                    userData.put("oauth_token", user.getOauthToken());
                }

                RequestBody body = RequestBody.create(userData.toString(), JSON);
                Request request = new Request.Builder()
                    .url(baseUrl + TABLE_NAME)
                    .addHeader("apikey", apiKey)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        if (jsonArray.length() > 0) {
                            return JsonUtils.jsonToUser(jsonArray.getJSONObject(0));
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Failed to create user. Status: " + response.code() + ", Error: " + errorBody);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error creating user: " + e.getMessage(), e);
            }
            return null;
        }, executorService);
    }
}