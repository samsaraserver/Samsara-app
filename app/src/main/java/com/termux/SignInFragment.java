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
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.termux.app.BiometricHelper;
import com.termux.app.NavbarHelper;
import com.termux.app.home_page;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;

import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SignInFragment extends Fragment {
    private static final String TAG = "SignInFragment";
    private static final String PREFS_NAME = "SamsaraLoginPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL_USERNAME = "email_username";
    private static final String KEY_ENCRYPTED_PASSWORD = "encrypted_password";
    private static final String KEY_ENCRYPTION_KEY = "login_encryption_key";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{8,}$");

    private UserRepository userRepository;
    private EditText emailUsernameBox;
    private EditText passwordBox;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences loginPrefs;
    private BiometricHelper biometricHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        try {
            SupabaseConfig.initialize(requireContext());
            userRepository = new UserRepository();
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
        setupBiometricLogin(view);
    }

    private void setupBiometricHelper() {
        biometricHelper = new BiometricHelper(requireActivity(), new BiometricHelper.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccessful(String username, String email, String password, String userId) {
                loginWithStoredCredentials(username, email, password);
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
    }

    private void initializeViews(View view) {
        TextView tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setText(Html.fromHtml(tvForgotPassword.getText().toString()));
        }

        TextView tvRegister2 = view.findViewById(R.id.tvRegister2);
        if (tvRegister2 != null) {
            tvRegister2.setText(Html.fromHtml(tvRegister2.getText().toString()));
        }

        emailUsernameBox = view.findViewById(R.id.EmailUsernameBox);
        passwordBox = view.findViewById(R.id.PasswordBox);
        rememberMeCheckBox = view.findViewById(R.id.RememberMeCheckBox);
    }

    private void loadSavedCredentials() {
        boolean rememberMe = loginPrefs.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedUsername = loginPrefs.getString(KEY_EMAIL_USERNAME, "");
            emailUsernameBox.setText(savedUsername);
            passwordBox.setHint("Enter password");
            passwordBox.setText("");
            rememberMeCheckBox.setChecked(true);
            Toast.makeText(requireContext(), "Credentials saved. Enter password or use biometrics.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners(View view) {
        ImageButton registerBtn = view.findViewById(R.id.RegisterBtn);
        ImageButton registerBtn2 = view.findViewById(R.id.RegisterBtn2);

        View.OnClickListener toSignUp = v -> {
            Bundle result = new Bundle();
            result.putString("nav", "toSignUp");
            getParentFragmentManager().setFragmentResult("auth_nav", result);
        };

        if (registerBtn != null) registerBtn.setOnClickListener(toSignUp);
        if (registerBtn2 != null) registerBtn2.setOnClickListener(toSignUp);

        ImageButton biometricLoginButton = view.findViewById(R.id.LoginBiometricsBtn);
        if (biometricLoginButton != null) {
            biometricLoginButton.setOnClickListener(v -> {
                if (biometricHelper != null) {
                    if (biometricHelper.isBiometricAvailable() && biometricHelper.hasStoredCredentials()) {
                        biometricHelper.startBiometricAuthentication();
                    } else {
                        Toast.makeText(requireContext(), "Biometric authentication not available or no stored credentials.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Biometric helper not initialized.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ImageButton loginButton = view.findViewById(R.id.SignInBtn);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> handleLogin());
        }

        ImageButton continueWithoutAccountButton = view.findViewById(R.id.ContinueWithoutAccountBtn);
        if (continueWithoutAccountButton != null) {
            continueWithoutAccountButton.setOnClickListener(v -> {
                NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
            });
        }

        ImageButton githubButton = view.findViewById(R.id.LoginGithubGtn);
        if (githubButton != null) {
            githubButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "GitHub login not implemented yet", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBiometricLogin(View view) {
        if (biometricHelper == null) {
            Log.e(TAG, "BiometricHelper is null, cannot setup biometric login");
            return;
        }

        if (!biometricHelper.isBiometricAvailable()) {
            ImageButton biometricLoginButton = view.findViewById(R.id.LoginBiometricsBtn);
            if (biometricLoginButton != null) {
                biometricLoginButton.setEnabled(false);
                biometricLoginButton.setAlpha(0.5f);
            }
        }
    }

    private void handleLogin() {
        String emailOrUsername = emailUsernameBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateLoginInput(emailOrUsername, password)) {
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
                                Toast.makeText(requireContext(), "Login error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
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
                Log.e(TAG, "Error during login", e);
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(requireContext(), "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleUserResult(SamsaraUser user, Throwable userThrowable, String emailOrUsername, String password) {
        if (userThrowable != null) {
            Log.e(TAG, "Error fetching user data", userThrowable);
            Toast.makeText(requireContext(), "Login error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
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
            AuthManager.getInstance(requireContext()).loginUser(user, password);
            Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
            NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
        } else {
            Toast.makeText(requireContext(), "Invalid email/username or password", Toast.LENGTH_LONG).show();
            passwordBox.setText("");
            passwordBox.requestFocus();
            setFormEnabled(true);
        }
    }

    private void storeCredentialsForBiometric(SamsaraUser user, String emailOrUsername, String password) {
        if (user == null || biometricHelper == null) {
            return;
        }

        if (biometricHelper.isBiometricAvailable()) {
            String username = user.getUsername();
            String email = user.getEmail();
            String userId = String.valueOf(user.getId());
            biometricHelper.storeCredentials(username, email, password, userId);
            Toast.makeText(requireContext(), "Biometric login updated and ready to use!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithStoredCredentials(String username, String email, String password) {
        setFormEnabled(false);
        Toast.makeText(requireContext(), "Logging in with biometrics...", Toast.LENGTH_SHORT).show();

        final String loginIdentifier = !TextUtils.isEmpty(email) ? email : username;

        if (TextUtils.isEmpty(loginIdentifier) || TextUtils.isEmpty(password)) {
            Log.e(TAG, "Stored credentials are incomplete");
            Toast.makeText(requireContext(), "Stored login information is incomplete. Please log in with password.", Toast.LENGTH_LONG).show();
            setFormEnabled(true);
            return;
        }

        new Thread(() -> {
            try {
                userRepository.authenticateUser(loginIdentifier, password)
                    .handle((success, throwable) -> {
                        if (throwable != null) {
                            requireActivity().runOnUiThread(() -> {
                                setFormEnabled(true);
                                Log.e(TAG, "Error during biometric login", throwable);
                                Toast.makeText(requireContext(), "Biometric login error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            });
                            return null;
                        }

                        if (success != null && success) {
                            new Thread(() -> {
                                try {
                                    SamsaraUser user = null;
                                    if (loginIdentifier.contains("@")) {
                                        user = userRepository.getUserByEmail(loginIdentifier).get();
                                    } else {
                                        user = userRepository.getUserByUsername(loginIdentifier).get();
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
                                Toast.makeText(requireContext(), "Biometric login failed. Your stored credentials may no longer be valid.", Toast.LENGTH_LONG).show();
                            });
                        }
                        return null;
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error during biometric login", e);
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(requireContext(), "Biometric login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleBiometricUserResult(SamsaraUser user, Throwable userThrowable, String password) {
        setFormEnabled(true);
        if (userThrowable != null) {
            Log.e(TAG, "Error fetching user data for biometric login", userThrowable);
            Toast.makeText(requireContext(), "Biometric login error: " + userThrowable.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (user != null) {
            if (!TextUtils.isEmpty(password)) {
                AuthManager.getInstance(requireContext()).loginUser(user, password);
            } else {
                AuthManager.getInstance(requireContext()).loginUser(user);
            }
            Toast.makeText(requireContext(), "Biometric login successful!", Toast.LENGTH_SHORT).show();
            NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
        } else {
            Toast.makeText(requireContext(), "Biometric login failed. Your stored credentials may no longer be valid.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateLoginInput(String emailOrUsername, String password) {
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
            SharedPreferences.Editor editor = loginPrefs.edit();
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
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.remove(KEY_REMEMBER_ME);
        editor.remove(KEY_EMAIL_USERNAME);
        editor.remove(KEY_ENCRYPTED_PASSWORD);
        editor.remove(KEY_ENCRYPTION_KEY);
        editor.apply();
    }

    private SecretKey getOrCreateEncryptionKey() throws Exception {
        String encodedKey = loginPrefs.getString(KEY_ENCRYPTION_KEY, null);
        if (encodedKey != null) {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            return new SecretKeySpec(keyBytes, "AES");
        }

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        String encodedNewKey = Base64.getEncoder().encodeToString(key.getEncoded());
        loginPrefs.edit().putString(KEY_ENCRYPTION_KEY, encodedNewKey).apply();

        return key;
    }

    private String encryptPassword(String password, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decryptPassword(String encryptedPassword, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(decryptedBytes);
    }

    private String retrieveSavedPassword() {
        try {
            String encryptedPassword = loginPrefs.getString(KEY_ENCRYPTED_PASSWORD, null);
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

            if (userRepository != null) {
                userRepository.shutdown();
                userRepository = null;
            }

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
