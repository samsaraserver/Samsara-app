package com.termux.app.database.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.termux.app.database.models.SamsaraUser;

public class AuthManager {
    private static final String PREFS_NAME = "samsara_auth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PROFILE_PICTURE_URL = "profile_picture_url";

    private static AuthManager instance;
    private SharedPreferences prefs;
    private SamsaraUser currentUser;

    private AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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

    public void logoutUser() {
        this.currentUser = null;
        clearUserFromPrefs();
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
}