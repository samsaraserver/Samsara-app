package com.termux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class WelcomePageActivity extends AppCompatActivity {
    private static final String TAG = "WelcomePageActivity";
    private static final String PREFS_NAME = "SamsaraLoginPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL_USERNAME = "email_username";
    private static final String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    private static final String KEY_ENCRYPTION_KEY = "login_encryption_key";

    private UserRepository userRepository;
    private SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        try {
            SupabaseConfig.initialize(this);
            userRepository = new UserRepository();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Supabase: " + e.getMessage(), e);
        }

        if (checkAutoLogin()) {
            return;
        }

        setContentView(R.layout.welcome_page);

        ImageButton getStartedButton = findViewById(R.id.WelcomeBtn);
        
        getStartedButton.setOnClickListener(view -> {
            NavbarHelper.navigateToActivity(WelcomePageActivity.this, SignInOut_page.class);
        });
    }

    private boolean checkAutoLogin() {
        boolean rememberMe = loginPrefs.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe && userRepository != null) {
            String savedUsername = loginPrefs.getString(KEY_EMAIL_USERNAME, "");
            String savedPassword = retrieveSavedPassword();
            
            if (!TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
                performAutoLogin(savedUsername, savedPassword);
                return true;
            }
        }
        return false;
    }

    private String retrieveSavedPassword() {
        try {
            String encryptedPassword = loginPrefs.getString(KEY_ENCRYPTED_PASSWORD, null);
            if (encryptedPassword != null) {
                SecretKey encryptionKey = getEncryptionKey();
                if (encryptionKey != null) {
                    return decryptPassword(encryptedPassword, encryptionKey);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving saved password", e);
        }
        return null;
    }

    private SecretKey getEncryptionKey() {
        try {
            String encodedKey = loginPrefs.getString(KEY_ENCRYPTION_KEY, null);
            if (encodedKey != null) {
                byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
                return new SecretKeySpec(keyBytes, "AES");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting encryption key", e);
        }
        return null;
    }

    private String decryptPassword(String encryptedPassword, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(decryptedBytes);
    }

    private void performAutoLogin(String emailOrUsername, String password) {
        if (TextUtils.isEmpty(emailOrUsername) || TextUtils.isEmpty(password)) {
            return;
        }

        new Thread(() -> {
            try {
                userRepository.authenticateUser(emailOrUsername, password)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            runOnUiThread(() -> Log.e(TAG, "Auto-login failed", throwable));
                            return null;
                        }

                        if (success != null && success) {
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (emailOrUsername.contains("@")) {
                                        user = userRepository.getUserByEmail(emailOrUsername).get();
                                    } else {
                                        user = userRepository.getUserByUsername(emailOrUsername).get();
                                    }

                                    final SamsaraUser finalUser = user;
                                    runOnUiThread(() -> handleAutoLoginSuccess(finalUser, password));
                                } catch (Exception e) {
                                    runOnUiThread(() -> Log.e(TAG, "Error fetching user data", e));
                                }
                            }).start();
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during auto-login", e);
            }
        }).start();
    }

    private void handleAutoLoginSuccess(SamsaraUser user, String password) {
        if (user != null) {
            AuthManager.getInstance(this).loginUser(user, password);
            NavbarHelper.navigateToActivity(this, home_page.class);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRepository != null) {
            userRepository.shutdown();
            userRepository = null;
        }
    }
}
