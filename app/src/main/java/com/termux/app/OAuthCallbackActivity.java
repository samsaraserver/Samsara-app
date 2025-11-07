package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.models.SamsaraUser;
import com.termux.app.home_page;
import com.termux.app.oauth.GitHubOAuthManager;


public class OAuthCallbackActivity extends Activity {
    private static final String TAG = "OAuthCallbackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data != null) {
            String code = data.getQueryParameter("code");
            String error = data.getQueryParameter("error");

            if (error != null) {
                Log.e(TAG, "OAuth error: " + error);
                Toast.makeText(this, "GitHub authentication failed: " + error, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (code != null) {
                Log.d(TAG, "Received authorization code, completing OAuth flow");
                handleOAuthCallback(code);
            } else {
                Log.e(TAG, "No authorization code received");
                Toast.makeText(this, "Authentication failed: No authorization code", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Log.e(TAG, "No data received in callback");
            Toast.makeText(this, "Authentication failed: Invalid callback", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleOAuthCallback(String code) {
        GitHubOAuthManager oauthManager = GitHubOAuthManager.getInstance(this);
        UserRepository userRepository = UserRepository.getInstance(this);

        oauthManager.completeOAuthFlow(code)
                .thenCompose(githubUserInfo -> {
                    Log.d(TAG, "GitHub user info received: " + githubUserInfo);

                    return userRepository.findByGithubId(githubUserInfo.getGithubId())
                            .thenCompose(existingUser -> {
                                if (existingUser != null) {
                                    Log.d(TAG, "Existing user found, logging in");
                                    return userRepository.findById(existingUser.getId());
                                } else {
                                    Log.d(TAG, "New user, creating account");
                                    return createGitHubUser(userRepository, githubUserInfo);
                                }
                            });
                })
                .thenAccept(user -> {
                    if (user != null) {
                        runOnUiThread(() -> {
                            AuthManager.getInstance(this).signinUser(user);

                            storeBiometricCredentialsForGitHubUser(user);

                            Toast.makeText(this, "Successfully signed in with GitHub!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, home_page.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to authenticate with GitHub", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "OAuth flow failed", throwable);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Authentication error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return null;
                });
    }

    private java.util.concurrent.CompletableFuture<SamsaraUser> createGitHubUser(
            UserRepository userRepository,
            GitHubOAuthManager.GitHubUserInfo githubUserInfo) {

        return userRepository.isUsernameAvailable(githubUserInfo.getUsername())
                .thenCompose(usernameAvailable -> {
                    String username = usernameAvailable ? githubUserInfo.getUsername() :
                                     githubUserInfo.getUsername() + "_" + githubUserInfo.getGithubId().substring(0, 4);

                    if (githubUserInfo.getEmail() != null) {
                        return userRepository.isEmailAvailable(githubUserInfo.getEmail())
                                .thenCompose(emailAvailable -> {
                                    if (!emailAvailable) {
                                        return userRepository.findByEmail(githubUserInfo.getEmail())
                                                .thenCompose(existingUser -> {
                                                    if (existingUser != null) {
                                                        existingUser.setGithubId(githubUserInfo.getGithubId());
                                                        existingUser.setAuthProvider("github");
                                                        existingUser.setOauthToken(githubUserInfo.getAccessToken());
                                                        existingUser.setProfilePictureUrl(githubUserInfo.getAvatarUrl());
                                                        return userRepository.updateUser(existingUser);
                                                    }
                                                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                                                });
                                    } else {
                                        return createNewGitHubUser(userRepository, githubUserInfo, username);
                                    }
                                });
                    } else {
                        return createNewGitHubUser(userRepository, githubUserInfo, username);
                    }
                });
    }

    private java.util.concurrent.CompletableFuture<SamsaraUser> createNewGitHubUser(
            UserRepository userRepository,
            GitHubOAuthManager.GitHubUserInfo githubUserInfo,
            String username) {

        SamsaraUser newUser = new SamsaraUser();
        newUser.setUsername(username);
        newUser.setEmail(githubUserInfo.getEmail());
        newUser.setPasswordHash(null);
        newUser.setGithubId(githubUserInfo.getGithubId());
        newUser.setAuthProvider("github");
        newUser.setOauthToken(githubUserInfo.getAccessToken());
        newUser.setProfilePictureUrl(githubUserInfo.getAvatarUrl());
        newUser.setIsActive(true);

        return userRepository.createUser(newUser);
    }

    private void storeBiometricCredentialsForGitHubUser(SamsaraUser user) {
        try {
            androidx.biometric.BiometricManager biometricManager =
                androidx.biometric.BiometricManager.from(this);

            int canAuthenticate = biometricManager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG);

            if (canAuthenticate != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                Log.d(TAG, "Biometric authentication not available, skipping credential storage");
                return;
            }

            // Store credentials using SharedPreferences directly
            android.content.SharedPreferences prefs =
                getSharedPreferences("BiometricSignupPrefs", MODE_PRIVATE);

            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("biometric_username", user.getUsername());
            editor.putString("biometric_email", user.getEmail());
            editor.putString("biometric_password", "__OAUTH_USER__");
            editor.putString("biometric_user_id", String.valueOf(user.getId()));
            editor.putString("biometric_auth_provider", user.getAuthProvider());
            editor.apply();

            Log.d(TAG, "Stored biometric credentials for GitHub user: " + user.getUsername());
        } catch (Exception e) {
            Log.e(TAG, "Error storing biometric credentials for GitHub user", e);
        }
    }
}

