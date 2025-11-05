package com.termux.app.oauth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class GitHubOAuthManager {
    private static final String TAG = "GitHubOAuthManager";
    private static final String GITHUB_USER_API = "https://api.github.com/user";
    private static final String GITHUB_EMAIL_API = "https://api.github.com/user/emails";

    private static GitHubOAuthManager instance;
    private OAuth20Service service;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    private GitHubOAuthManager(Context context) {
        loadConfiguration(context);
        initializeService();
    }

    public static synchronized GitHubOAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new GitHubOAuthManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadConfiguration(Context context) {
        try {
            Properties properties = new Properties();
            InputStream inputStream = context.getAssets().open("supabase.properties");
            properties.load(inputStream);

            clientId = properties.getProperty("GITHUB_CLIENT_ID");
            clientSecret = properties.getProperty("GITHUB_CLIENT_SECRET");
            redirectUri = properties.getProperty("GITHUB_REDIRECT_URI");

            if (clientId == null || clientSecret == null || redirectUri == null) {
                throw new IllegalStateException("GitHub OAuth credentials not found in supabase.properties");
            }

            Log.d(TAG, "GitHub OAuth configuration loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load GitHub OAuth configuration", e);
            throw new RuntimeException("Failed to load GitHub OAuth configuration", e);
        }
    }


    private void initializeService() {
        service = new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .callback(redirectUri)
                .defaultScope("user:email read:user")
                .build(GitHubApi.instance());
    }

    public String getAuthorizationUrl() {
        return service.getAuthorizationUrl();
    }

    public void startOAuthFlow(Context context) {
        String authUrl = getAuthorizationUrl();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        context.startActivity(browserIntent);
        Log.d(TAG, "Started OAuth flow with URL: " + authUrl);
    }

    public CompletableFuture<OAuth2AccessToken> exchangeCodeForToken(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OAuth2AccessToken accessToken = service.getAccessToken(code);
                Log.d(TAG, "Successfully exchanged code for access token");
                return accessToken;
            } catch (Exception e) {
                Log.e(TAG, "Failed to exchange code for token", e);
                throw new RuntimeException("Failed to exchange authorization code for token", e);
            }
        });
    }

    public CompletableFuture<GitHubUserInfo> getUserInfo(OAuth2AccessToken accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get user profile
                OAuthRequest request = new OAuthRequest(Verb.GET, GITHUB_USER_API);
                service.signRequest(accessToken, request);
                Response response = service.execute(request);

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get user info: " + response.getCode());
                }

                JSONObject userJson = new JSONObject(response.getBody());

                OAuthRequest emailRequest = new OAuthRequest(Verb.GET, GITHUB_EMAIL_API);
                service.signRequest(accessToken, emailRequest);
                Response emailResponse = service.execute(emailRequest);

                String primaryEmail = null;
                if (emailResponse.isSuccessful()) {
                    org.json.JSONArray emails = new org.json.JSONArray(emailResponse.getBody());
                    for (int i = 0; i < emails.length(); i++) {
                        JSONObject emailObj = emails.getJSONObject(i);
                        if (emailObj.optBoolean("primary", false)) {
                            primaryEmail = emailObj.getString("email");
                            break;
                        }
                    }
                }

                // If no primary email found, use the first available email or null
                if (primaryEmail == null && !emailResponse.getBody().isEmpty()) {
                    org.json.JSONArray emails = new org.json.JSONArray(emailResponse.getBody());
                    if (emails.length() > 0) {
                        primaryEmail = emails.getJSONObject(0).getString("email");
                    }
                }

                GitHubUserInfo userInfo = new GitHubUserInfo();
                userInfo.setGithubId(userJson.getString("id"));
                userInfo.setUsername(userJson.optString("login", null));
                userInfo.setEmail(primaryEmail);
                userInfo.setName(userJson.optString("name", null));
                userInfo.setAvatarUrl(userJson.optString("avatar_url", null));
                userInfo.setAccessToken(accessToken.getAccessToken());

                Log.d(TAG, "Successfully retrieved user info for: " + userInfo.getUsername());
                return userInfo;

            } catch (Exception e) {
                Log.e(TAG, "Failed to get user info", e);
                throw new RuntimeException("Failed to get GitHub user info", e);
            }
        });
    }

    public CompletableFuture<GitHubUserInfo> completeOAuthFlow(String code) {
        return exchangeCodeForToken(code)
                .thenCompose(this::getUserInfo);
    }

    public static class GitHubUserInfo {
        private String githubId;
        private String username;
        private String email;
        private String name;
        private String avatarUrl;
        private String accessToken;

        public String getGithubId() {
            return githubId;
        }

        public void setGithubId(String githubId) {
            this.githubId = githubId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String toString() {
            return "GitHubUserInfo{" +
                    "githubId='" + githubId + '\'' +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
