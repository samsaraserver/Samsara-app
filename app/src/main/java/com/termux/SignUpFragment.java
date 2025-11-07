package com.termux;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.BiometricHelper;
import com.termux.app.oauth.GitHubOAuthManager;

import java.util.concurrent.CompletableFuture;

public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";

    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailBox;
    private EditText passwordBox;
    private CheckBox checkBoxTermsAndConditions;
    private BiometricHelper biometricHelper;

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
        checkBoxTermsAndConditions = view.findViewById(R.id.CheckBoxTermsAndConditions);
    }

    private void setupClickListeners(View view) {
        ImageButton signinBtn = view.findViewById(R.id.SignInBtn);
        ImageButton signinBtn2 = view.findViewById(R.id.SignInBtn2);

        View.OnClickListener toSignIn = v -> {
            Bundle result = new Bundle();
            result.putString("nav", "toSignIn");
            getParentFragmentManager().setFragmentResult("auth_nav", result);
        };

        if (signinBtn != null) signinBtn.setOnClickListener(toSignIn);
        if (signinBtn2 != null) signinBtn2.setOnClickListener(toSignIn);

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
                        AuthManager.getInstance(requireContext()).signinUser(user, password);
                        try {
                            if (biometricHelper != null) {
                                biometricHelper.storeCredentials(user.getUsername(), user.getEmail(), password, String.valueOf(user.getId()));
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error storing biometric credentials after signup", ex);
                        }
                        Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_LONG).show();
                        NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
                    } else {
                        Toast.makeText(requireContext(), "Account may have been created, but sign in failed. Please try signing in manually.", Toast.LENGTH_LONG).show();
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
        // Don't shutdown UserRepository as it's a singleton used across the app
        userRepository = null;
        if (biometricHelper != null) {
            try { biometricHelper.cleanup(); } catch (Exception ignore) {}
            biometricHelper = null;
        }
        usernameBox = null;
        emailBox = null;
        passwordBox = null;
        checkBoxTermsAndConditions = null;
    }
}
