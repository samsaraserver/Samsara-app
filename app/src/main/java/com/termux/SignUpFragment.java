package com.termux;

import android.content.Intent;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.app.NavbarHelper;
import com.termux.app.home_page;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.BiometricHelper;
import com.termux.app.database.repository.UserRepository;

import java.util.concurrent.CompletableFuture;
import com.termux.app.oauth.GitHubOAuthManager;

@SuppressLint("NewApi")
public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";

    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailBox;
    private EditText passwordBox;
    private EditText confirmPasswordBox;
    private CheckBox checkBoxTermsAndConditions;
    private BiometricHelper biometricHelper;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        return inflater.inflate(R.layout.signup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners(view);

        // Initialize biometric helper for storing credentials after successful signup
        biometricHelper = new BiometricHelper(requireActivity(), new BiometricHelper.AuthenticationCallback() {
            @Override public void onAuthenticationSuccessful(String u, String e, String p, String id) { /* no-op */ }
            @Override public void onAuthenticationFailed() { /* no-op */ }
            @Override public void onAuthenticationError(int errorCode, CharSequence errString) { /* no-op */ }
        });
    }

    private void initializeViews(View view) {
        usernameBox = view.findViewById(R.id.UsernameBox);
        emailBox = view.findViewById(R.id.EmailPhoneBox);
        passwordBox = view.findViewById(R.id.PasswordBox);
        confirmPasswordBox = view.findViewById(R.id.ConfirmPasswordBox);
        checkBoxTermsAndConditions = view.findViewById(R.id.CheckBoxTermsAndConditions);
    }

    private void setupClickListeners(View view) {
        ImageButton loginBtn = view.findViewById(R.id.LoginBtn);
        ImageButton loginBtn2 = view.findViewById(R.id.LoginBtn2);

        View.OnClickListener toSignIn = v -> {
            Bundle result = new Bundle();
            result.putString("nav", "toSignIn");
            getParentFragmentManager().setFragmentResult("auth_nav", result);
        };

        if (loginBtn != null) loginBtn.setOnClickListener(toSignIn);
        if (loginBtn2 != null) loginBtn2.setOnClickListener(toSignIn);

        ImageButton togglePasswordVisibilityBtn = view.findViewById(R.id.TogglePasswordVisibilityBtn);
        if (togglePasswordVisibilityBtn != null) {
            togglePasswordVisibilityBtn.setOnClickListener(v -> togglePasswordVisibility());
        }

        ImageButton toggleConfirmPasswordVisibilityBtn = view.findViewById(R.id.ToggleConfirmPasswordVisibilityBtn);
        if (toggleConfirmPasswordVisibilityBtn != null) {
            toggleConfirmPasswordVisibilityBtn.setOnClickListener(v -> toggleConfirmPasswordVisibility());
        }

        ImageButton createAccountButton = view.findViewById(R.id.CreateAccountBtn);
        if (createAccountButton != null) {
            createAccountButton.setOnClickListener(v -> handleCreateAccount());
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
                    Toast.makeText(requireContext(), "GitHub signup error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void handleCreateAccount() {
        String username = usernameBox.getText().toString().trim();
        String email = emailBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateInput(username, email, password)) {
            return;
        }

        setFormEnabled(false);
        Toast.makeText(requireContext(), "Checking availability...", Toast.LENGTH_SHORT).show();

        userRepository.checkUsernameExists(username)
            .thenCompose(usernameExists -> {
                if (usernameExists) {
                    requireActivity().runOnUiThread(() -> {
                        setFormEnabled(true);
                        usernameBox.setError("Username already exists");
                        usernameBox.requestFocus();
                    });
                    return CompletableFuture.completedFuture(false);
                }
                return userRepository.checkEmailExists(email);
            })
            .thenCompose(emailExists -> {
                if (emailExists) {
                    requireActivity().runOnUiThread(() -> {
                        setFormEnabled(true);
                        emailBox.setError("Email already registered");
                        emailBox.requestFocus();
                    });
                    return CompletableFuture.completedFuture(false);
                }

                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Creating account...", Toast.LENGTH_SHORT).show());
                return userRepository.createUser(username, email, password);
            })
            .thenCompose(success -> {
                if (success) {
                    return userRepository.getUserByEmail(email);
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            })
            .thenAccept(user -> {
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    if (user != null) {
                        AuthManager.getInstance(requireContext()).loginUser(user, password);
                        try {
                            if (biometricHelper != null) {
                                biometricHelper.storeCredentials(user.getUsername(), user.getEmail(), password, String.valueOf(user.getId()));
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error storing biometric credentials after signup", ex);
                        }
                        Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(requireActivity(), home_page.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        requireActivity().finish();
                    } else {
                        Toast.makeText(requireContext(), "Account may have been created, but login failed. Please try signing in manually.", Toast.LENGTH_LONG).show();
                    }
                });
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error creating account", throwable);
                requireActivity().runOnUiThread(() -> {
                    setFormEnabled(true);
                    Toast.makeText(requireContext(), "Error creating account: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }

    private boolean validateInput(String username, String email, String password) {
        if (TextUtils.isEmpty(username)) {
            usernameBox.setError("Username is required");
            usernameBox.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            usernameBox.setError("Username must be at least 3 characters");
            usernameBox.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailBox.setError("Email is required");
            emailBox.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Please enter a valid email address");
            emailBox.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordBox.setError("Password is required");
            passwordBox.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            passwordBox.setError("Password must be at least 8 characters");
            passwordBox.requestFocus();
            return false;
        }

        if (!checkBoxTermsAndConditions.isChecked()) {
            Toast.makeText(requireContext(), "You must agree to the Terms and Conditions", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void setFormEnabled(boolean enabled) {
        if (usernameBox != null) usernameBox.setEnabled(enabled);
        if (emailBox != null) emailBox.setEnabled(enabled);
        if (passwordBox != null) passwordBox.setEnabled(enabled);
        if (getView() != null) {
            View createBtn = getView().findViewById(R.id.CreateAccountBtn);
            if (createBtn != null) createBtn.setEnabled(enabled);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRepository != null) {
            userRepository.shutdown();
            userRepository = null;
        }
        if (biometricHelper != null) {
            try { biometricHelper.cleanup(); } catch (Exception ignore) {}
            biometricHelper = null;
        }
        usernameBox = null;
        emailBox = null;
        passwordBox = null;
        confirmPasswordBox = null;
        checkBoxTermsAndConditions = null;
    }

    private void togglePasswordVisibility() {
        if (passwordBox == null) return;

        if (isPasswordVisible) {
            passwordBox.setTransformationMethod(PasswordTransformationMethod.getInstance());
            isPasswordVisible = false;
        } else {
            passwordBox.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            isPasswordVisible = true;
        }
        passwordBox.setSelection(passwordBox.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (confirmPasswordBox == null) return;

        if (isConfirmPasswordVisible) {
            confirmPasswordBox.setTransformationMethod(PasswordTransformationMethod.getInstance());
            isConfirmPasswordVisible = false;
        } else {
            confirmPasswordBox.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            isConfirmPasswordVisible = true;
        }
        confirmPasswordBox.setSelection(confirmPasswordBox.getText().length());
    }
}
