package com.termux.app.database.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.termux.app.database.models.SamsaraUser;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "samsara_auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PROFILE_PICTURE_URL = "profile_picture_url";
    private static final String KEY_PASSWORD_HASH = "password_hash";

    private static AuthManager instance;
    private SharedPreferences prefs;
    private SamsaraUser currentUser;
    private final Context appContext;

    private AuthManager(Context context) {
        this.appContext = context.getApplicationContext();
        prefs = this.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadUserFromPrefs();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loginUser(SamsaraUser user) {
        this.currentUser = user;
        saveUserToPrefs(user);
    }

    public void loginUser(SamsaraUser user, String password) {
        this.currentUser = user;
        saveUserToPrefs(user, password);
    }

    public void logoutUser() {
        this.currentUser = null;
        clearUserFromPrefs();
        clearSavedCredentials();
    }

    public boolean isLoggedIn() {
        return currentUser != null && prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public SamsaraUser getCurrentUser() {
        return currentUser;
    }

    private void saveUserToPrefs(SamsaraUser user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, user.getId() != null ? user.getId() : 0L);
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_PROFILE_PICTURE_URL, user.getProfilePictureUrl());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    private void saveUserToPrefs(SamsaraUser user, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, user.getId() != null ? user.getId() : 0L);
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_PROFILE_PICTURE_URL, user.getProfilePictureUrl());
        editor.putString(KEY_PASSWORD_HASH, hashPassword(password));
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean validatePassword(String password) {
        if (!isLoggedIn()) {
            return false;
        }
        String storedHash = prefs.getString(KEY_PASSWORD_HASH, null);
        if (storedHash == null) {
            return false;
        }
        return storedHash.equals(hashPassword(password));
    }

    private String hashPassword(String password) {
        // Simple hash for now - in production you'd want to use proper password hashing
        return String.valueOf(password.hashCode());
    }

    private void loadUserFromPrefs() {
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            currentUser = new SamsaraUser();
            currentUser.setId(prefs.getLong(KEY_USER_ID, 0L));
            currentUser.setUsername(prefs.getString(KEY_USERNAME, null));
            currentUser.setEmail(prefs.getString(KEY_EMAIL, null));
            currentUser.setProfilePictureUrl(prefs.getString(KEY_PROFILE_PICTURE_URL, null));
        }
    }

    private void clearUserFromPrefs() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void clearSavedCredentials() {
        try {
            SharedPreferences loginPrefs = appContext.getSharedPreferences("SamsaraLoginPrefs", Context.MODE_PRIVATE);
            loginPrefs.edit().clear().apply();

            SharedPreferences biometricPrefs = appContext.getSharedPreferences("BiometricLoginPrefs", Context.MODE_PRIVATE);
            biometricPrefs.edit().clear().apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear stored credentials on logout", e);
        }
    }
}
