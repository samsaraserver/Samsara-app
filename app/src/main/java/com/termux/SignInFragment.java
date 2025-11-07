package com.termux;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.termux.app.BiometricHelper;
import com.termux.app.NavbarHelper;
import com.termux.app.home_page;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;
import com.termux.app.oauth.GitHubOAuthManager;

import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SignInFragment extends Fragment {
    private static final String TAG = "SignInFragment";
    private static final String PREFS_NAME = "SamsaraSignInPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL_USERNAME = "email_username";
    private static final String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    private static final String KEY_ENCRYPTION_KEY = "signin_encryption_key";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{8,}$");

    private UserRepository userRepository;
    private EditText emailUsernameBox;
    private EditText passwordBox;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences signInPrefs;
    private BiometricHelper biometricHelper;
    private BiometricPrompt biometricPrompt;
    private SharedPreferences biometricPrefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        signInPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        biometricPrefs = requireActivity().getSharedPreferences("BiometricSignupPrefs", Context.MODE_PRIVATE);

        try {
            SupabaseConfig.initialize(requireContext());
            userRepository = UserRepository.getInstance(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Supabase: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Database connection error. Please try again later.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.signin_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        loadSavedCredentials();
        setupClickListeners(view);
        setupBiometricHelper();
        setupBiometricSignIn(view);

        // Debug: Check what's stored in biometric prefs
        logStoredBiometricInfo();
    }

    private void logStoredBiometricInfo() {
        Log.d(TAG, "=== Biometric Credentials Debug ===");
        Log.d(TAG, "Has biometric_username: " + biometricPrefs.contains("biometric_username"));
        Log.d(TAG, "Has biometric_password: " + biometricPrefs.contains("biometric_password"));
        Log.d(TAG, "Has biometric_email: " + biometricPrefs.contains("biometric_email"));
        Log.d(TAG, "Has biometric_user_id: " + biometricPrefs.contains("biometric_user_id"));

        if (biometricPrefs.contains("biometric_username")) {
            Log.d(TAG, "Stored username: " + biometricPrefs.getString("biometric_username", ""));
        }
        if (biometricPrefs.contains("biometric_email")) {
            Log.d(TAG, "Stored email: " + biometricPrefs.getString("biometric_email", ""));
        }

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuthenticateStrong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        int canAuthenticateWeak = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);

        Log.d(TAG, "BIOMETRIC_STRONG status: " + canAuthenticateStrong + " (0=SUCCESS)");
        Log.d(TAG, "BIOMETRIC_WEAK status: " + canAuthenticateWeak + " (0=SUCCESS)");
        Log.d(TAG, "BiometricHelper.isBiometricAvailable(): " + (biometricHelper != null ? biometricHelper.isBiometricAvailable() : "helper is null"));
        Log.d(TAG, "===================================");
    }

    private void setupBiometricHelper() {
        biometricHelper = new BiometricHelper(requireActivity(), new BiometricHelper.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                signInWithStoredCredentials(username, email, password);
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(requireContext(), "Biometric authentication failed. Please try again or use your password.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(requireContext(), "Authentication error: " + errString, Toast.LENGTH_LONG).show();
                }
            }
        });

        // Setup BiometricPrompt for sign-in (simplified, like biometrics_page)
        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(requireActivity());
        biometricPrompt = new BiometricPrompt(requireActivity(), executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                retrieveAndSignInWithBiometricCredentials();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(requireContext(), "Authentication error: " + errString, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(requireContext(), "Biometric authentication failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasStoredBiometricCredentials() {
        return biometricPrefs.contains("biometric_username") && biometricPrefs.contains("biometric_password");
    }

    private void retrieveAndSignInWithBiometricCredentials() {
        Log.d(TAG, "retrieveAndSignInWithBiometricCredentials called");
        try {
            String username = biometricPrefs.getString("biometric_username", "");
            String email = biometricPrefs.getString("biometric_email", "");
            String password = biometricPrefs.getString("biometric_password", null);

            Log.d(TAG, "Retrieved credentials - Username: " + username + ", Email: " + email + ", Password exists: " + (password != null));

            if (password != null) {
                Log.d(TAG, "Credentials found, proceeding with sign-in");
                signInWithStoredCredentials(username, email, password);
            } else {
                Log.w(TAG, "Password is null in stored biometric credentials");
                Toast.makeText(requireContext(), "Biometric credentials not found. Please sign in with password.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving biometric credentials", e);
            Toast.makeText(requireContext(), "Error retrieving credentials. Please sign in with password.", Toast.LENGTH_LONG).show();
        }
    }

    private void startDirectBiometricAuthentication() {
        if (!hasStoredBiometricCredentials()) {
            Toast.makeText(requireContext(), "No biometric credentials saved. Please log in with password first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (biometricPrompt == null) {
            Toast.makeText(requireContext(), "Biometric prompt not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sign In")
                .setSubtitle("Use your biometric to sign in")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void initializeViews(View view) {
        TextView tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setText(Html.fromHtml(tvForgotPassword.getText().toString()));
        }

        TextView tvSignUp2 = view.findViewById(R.id.tvSignUp2);
        if (tvSignUp2 != null) {
            tvSignUp2.setText(Html.fromHtml(tvSignUp2.getText().toString()));
        }

        emailUsernameBox = view.findViewById(R.id.EmailUsernameBox);
        passwordBox = view.findViewById(R.id.PasswordBox);
        rememberMeCheckBox = view.findViewById(R.id.RememberMeCheckBox);
    }

    private void loadSavedCredentials() {
        boolean rememberMe = signInPrefs.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedUsername = signInPrefs.getString(KEY_EMAIL_USERNAME, "");
            emailUsernameBox.setText(savedUsername);
            passwordBox.setHint("Enter password");
            passwordBox.setText("");
            rememberMeCheckBox.setChecked(true);
            Toast.makeText(requireContext(), "Credentials saved. Enter password or use biometrics.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners(View view) {
        ImageButton signUpBtn = view.findViewById(R.id.SignUpBtn);
        ImageButton signUpBtn2 = view.findViewById(R.id.SignUpBtn2);

        View.OnClickListener toSignUp = v -> {
            Bundle result = new Bundle();
            result.putString("nav", "toSignUp");
            getParentFragmentManager().setFragmentResult("auth_nav", result);
        };

        if (signUpBtn != null) signUpBtn.setOnClickListener(toSignUp);
        if (signUpBtn2 != null) signUpBtn2.setOnClickListener(toSignUp);

        ImageButton biometricSignInButton = view.findViewById(R.id.SignInBiometricsBtn);
        if (biometricSignInButton != null) {
            biometricSignInButton.setOnClickListener(v -> {
                startDirectBiometricAuthentication();
            });
        }

        ImageButton signInButton = view.findViewById(R.id.SignInBtn);
        if (signInButton != null) {
            signInButton.setOnClickListener(v -> handleSignIn());
        }

        ImageButton continueWithoutAccountButton = view.findViewById(R.id.ContinueWithoutAccountBtn);
        if (continueWithoutAccountButton != null) {
            continueWithoutAccountButton.setOnClickListener(v -> {
                NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
            });
        }

        ImageButton githubButton = view.findViewById(R.id.SignInGithubBtn);
        if (githubButton != null) {
            githubButton.setOnClickListener(v -> {
                try {
                    GitHubOAuthManager oauthManager = GitHubOAuthManager.getInstance(requireContext());
                    oauthManager.startOAuthFlow(requireContext());
                    Toast.makeText(requireContext(), "Redirecting to GitHub...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start GitHub OAuth", e);
                    Toast.makeText(requireContext(), "GitHub sign in error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setupBiometricSignIn(View view) {
        if (biometricHelper == null) {
            Log.e(TAG, "BiometricHelper is null, cannot setup biometric sign in");
            return;
        }

        if (!biometricHelper.isBiometricAvailable()) {
            ImageButton biometricSignInButton = view.findViewById(R.id.SignInBiometricsBtn);
            if (biometricSignInButton != null) {
                biometricSignInButton.setEnabled(false);
                biometricSignInButton.setAlpha(0.5f);
            }
        }
    }

    private void handleSignIn() {
        String emailOrUsername = emailUsernameBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateSignInInput(emailOrUsername, password)) {
            return;
        }

        if (TextUtils.isEmpty(password) && rememberMeCheckBox.isChecked()) {
            password = retrieveSavedPassword();
            if (TextUtils.isEmpty(password)) {
                passwordBox.setError("Please enter your password");
                passwordBox.requestFocus();
                return;
            }
        }

        final String finalEmailOrUsername = emailOrUsername;
        final String finalPassword = password;

        setFormEnabled(false);
        Toast.makeText(requireContext(), "Signing in...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                userRepository.authenticateUser(finalEmailOrUsername, finalPassword)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            requireActivity().runOnUiThread(() -> {
                                setFormEnabled(true);
                                Log.e(TAG, "Authentication error", throwable);
                                Toast.makeText(requireContext(), "Sign in error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            });
                            return null;
                        }

                        if (success != null && success) {
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (finalEmailOrUsername.contains("@")) {
                                        user = userRepository.getUserByEmail(finalEmailOrUsername).get();
                                    } else {
                                        user = userRepository.getUserByUsername(finalEmailOrUsername).get();
                                    }

                                    final SamsaraUser finalUser = user;
                                    requireActivity().runOnUiThread(() -> handleUserResult(finalUser, null, finalEmailOrUsername, finalPassword));
                                } catch (Exception e) {
                                    requireActivity().runOnUiThread(() -> handleUserResult(null, e, finalEmailOrUsername, finalPassword));
                                }
                            }).start();
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                setFormEnabled(true);
                                Toast.makeText(requireContext(), "Invalid email/username or password", Toast.LENGTH_LONG).show();
                                passwordBox.setText("");
                                passwordBox.requestFocus();
                            });
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during sign in", e);
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(requireContext(), "Sign in error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleUserResult(SamsaraUser user, Throwable userThrowable, String emailOrUsername, String password) {
        if (userThrowable != null) {
            Log.e(TAG, "Error fetching user data", userThrowable);
            Toast.makeText(requireContext(), "Sign in error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
            setFormEnabled(true);
            return;
        }

        if (user != null) {
            if (rememberMeCheckBox.isChecked()) {
                saveCredentials(emailOrUsername, password);
            } else {
                clearSavedCredentials();
            }

            storeCredentialsForBiometric(user, emailOrUsername, password);
            AuthManager.getInstance(requireContext()).signinUser(user, password);
            Toast.makeText(requireContext(), "Sign in successful!", Toast.LENGTH_SHORT).show();
            NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
        } else {
            Toast.makeText(requireContext(), "Invalid email/username or password", Toast.LENGTH_LONG).show();
            passwordBox.setText("");
            passwordBox.requestFocus();
            setFormEnabled(true);
        }
    }

    private void storeCredentialsForBiometric(SamsaraUser user, String emailOrUsername, String password) {
        Log.d(TAG, "storeCredentialsForBiometric called");

        if (user == null) {
            Log.e(TAG, "Cannot store biometric credentials: user is null");
            return;
        }

        if (biometricHelper == null) {
            Log.e(TAG, "Cannot store biometric credentials: biometricHelper is null");
            return;
        }

        Log.d(TAG, "Checking if biometric is available...");
        if (biometricHelper.isBiometricAvailable()) {
            String username = user.getUsername();
            String email = user.getEmail();
            String userId = String.valueOf(user.getId());

            Log.d(TAG, "Biometric available. Storing credentials for user: " + username + " (ID: " + userId + ")");
            biometricHelper.storeCredentials(username, email, password, userId);

            // Verify credentials were stored
            if (biometricHelper.hasStoredCredentials()) {
                Log.d(TAG, "Credentials successfully stored and verified");
                Toast.makeText(requireContext(), "Biometric sign in updated and ready to use!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Credentials storage verification failed!");
                Toast.makeText(requireContext(), "Warning: Biometric credentials may not have been saved", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Biometric authentication not available on this device");
        }
    }

    private void signInWithStoredCredentials(String username, String email, String password) {
        setFormEnabled(false);
        Toast.makeText(requireContext(), "Signing in with biometrics...", Toast.LENGTH_SHORT).show();

        final String signInIdentifier = !TextUtils.isEmpty(email) ? email : username;

        if (TextUtils.isEmpty(signInIdentifier) || TextUtils.isEmpty(password)) {
            Log.e(TAG, "Stored credentials are incomplete");
            Toast.makeText(requireContext(), "Stored sign in information is incomplete. Please sign in with password.", Toast.LENGTH_LONG).show();
            setFormEnabled(true);
            return;
        }

        // Check if this is an OAuth user (GitHub user)
        boolean isOAuthUser = "__OAUTH_USER__".equals(password);

        if (isOAuthUser) {
            // For OAuth users, skip password authentication and directly fetch user data
            new Thread(() -> {
                try {
                    SamsaraUser user = null;
                    if (signInIdentifier.contains("@")) {
                        user = userRepository.getUserByEmail(signInIdentifier).get();
                    } else {
                        user = userRepository.getUserByUsername(signInIdentifier).get();
                    }

                    final SamsaraUser finalUser = user;
                    requireActivity().runOnUiThread(() -> {
                        if (finalUser != null) {
                            handleBiometricUserResult(finalUser, null, null);
                        } else {
                            setFormEnabled(true);
                            Toast.makeText(requireContext(), "User not found. Please sign in with GitHub again.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        setFormEnabled(true);
                        Log.e(TAG, "Error fetching OAuth user for biometric sign in", e);
                        Toast.makeText(requireContext(), "Biometric sign in error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
            return;
        }

        // Regular password-based authentication
        new Thread(() -> {
            try {
                userRepository.authenticateUser(signInIdentifier, password)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            requireActivity().runOnUiThread(() -> {
                                setFormEnabled(true);
                                Log.e(TAG, "Error during biometric sign in", throwable);
                                Toast.makeText(requireContext(), "Biometric sign in error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            });
                            return null;
                        }

                        if (success != null && success) {
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (signInIdentifier.contains("@")) {
                                        user = userRepository.getUserByEmail(signInIdentifier).get();
                                    } else {
                                        user = userRepository.getUserByUsername(signInIdentifier).get();
                                    }

                                    final SamsaraUser finalUser = user;
                                    requireActivity().runOnUiThread(() -> handleBiometricUserResult(finalUser, null, password));
                                } catch (Exception e) {
                                    requireActivity().runOnUiThread(() -> handleBiometricUserResult(null, e, null));
                                }
                            }).start();
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                setFormEnabled(true);
                                Toast.makeText(requireContext(), "Biometric sign in failed. Your stored credentials may no longer be valid.", Toast.LENGTH_LONG).show();
                            });
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during biometric sign in", e);
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(requireContext(), "Biometric sign in error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleBiometricUserResult(SamsaraUser user, Throwable userThrowable, String password) {
        setFormEnabled(true);
        if (userThrowable != null) {
            Log.e(TAG, "Error fetching user data for biometric sign in", userThrowable);
            Toast.makeText(requireContext(), "Biometric sign in error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (user != null) {
            if (!TextUtils.isEmpty(password)) {
                AuthManager.getInstance(requireContext()).signinUser(user, password);
            } else {
                AuthManager.getInstance(requireContext()).signinUser(user);
            }
            Toast.makeText(requireContext(), "Biometric sign in successful!", Toast.LENGTH_SHORT).show();
            NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
        } else {
            Toast.makeText(requireContext(), "Biometric sign in failed. Your stored credentials may no longer be valid.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateSignInInput(String emailOrUsername, String password) {
        if (TextUtils.isEmpty(emailOrUsername)) {
            emailUsernameBox.setError("Email or username is required");
            emailUsernameBox.requestFocus();
            return false;
        }

        if (emailOrUsername.contains("@")) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
                emailUsernameBox.setError("Please enter a valid email address");
                emailUsernameBox.requestFocus();
                return false;
            }
        }

        if (TextUtils.isEmpty(password) && !rememberMeCheckBox.isChecked()) {
            passwordBox.setError("Password is required");
            passwordBox.requestFocus();
            return false;
        }

        if (!TextUtils.isEmpty(password) && password.length() < 8) {
            passwordBox.setError("Password must be at least 8 characters");
            passwordBox.requestFocus();
            return false;
        }

        return true;
    }

    private void saveCredentials(String emailOrUsername, String password) {
        try {
            SharedPreferences.Editor editor = signInPrefs.edit();
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_EMAIL_USERNAME, emailOrUsername);

            SecretKey encryptionKey = getOrCreateEncryptionKey();
            String encryptedPassword = encryptPassword(password, encryptionKey);
            editor.putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword);

            editor.apply();
            Log.d(TAG, "Credentials saved securely with encryption");
        } catch (Exception e) {
            Log.e(TAG, "Error saving encrypted credentials", e);
            Toast.makeText(requireContext(), "Error saving credentials securely", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = signInPrefs.edit();
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_EMAIL_USERNAME);
        editor.remove(KEY_ENCRYPTED_PASSWORD);
        editor.remove(KEY_ENCRYPTION_KEY);
        editor.apply();
    }

    private SecretKey getOrCreateEncryptionKey() throws Exception {
        String encodedKey = signInPrefs.getString(KEY_ENCRYPTION_KEY, null);
        if (encodedKey != null) {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            return new SecretKeySpec(keyBytes, "AES");
        }

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        String encodedNewKey = Base64.getEncoder().encodeToString(key.getEncoded());
        signInPrefs.edit().putString(KEY_ENCRYPTION_KEY, encodedNewKey).apply();

        return key;
    }

    private String encryptPassword(String password, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decryptPassword(String encryptedPassword, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(decryptedBytes);
    }

    private String retrieveSavedPassword() {
        try {
            String encryptedPassword = signInPrefs.getString(KEY_ENCRYPTED_PASSWORD, null);
            if (encryptedPassword != null) {
                SecretKey encryptionKey = getOrCreateEncryptionKey();
                return decryptPassword(encryptedPassword, encryptionKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving saved password", e);
            Toast.makeText(requireContext(), "Error accessing saved password. Please enter manually.", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void setFormEnabled(boolean enabled) {
        if (emailUsernameBox != null) emailUsernameBox.setEnabled(enabled);
        if (passwordBox != null) passwordBox.setEnabled(enabled);
        if (getView() != null) {
            View signInBtn = getView().findViewById(R.id.SignInBtn);
            if (signInBtn != null) signInBtn.setEnabled(enabled);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (passwordBox != null) {
                passwordBox.setText("");
            }

            if (biometricHelper != null) {
                biometricHelper.cleanup();
                biometricHelper = null;
            }

            userRepository = null;

            emailUsernameBox = null;
            passwordBox = null;
            rememberMeCheckBox = null;
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroyView: " + e.getMessage(), e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (passwordBox != null && rememberMeCheckBox != null && !rememberMeCheckBox.isChecked()) {
            passwordBox.setText("");
        }
    }
}
