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

import java.util.concurrent.CompletableFuture;

public class SignUpFragment extends Fragment {
    private static final String TAG = "SignUpFragment";

    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailBox;
    private EditText passwordBox;
    private CheckBox checkBoxTermsAndConditions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        return inflater.inflate(R.layout.signup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners(view);
    }

    private void initializeViews(View view) {
        usernameBox = view.findViewById(R.id.UsernameBox);
        emailBox = view.findViewById(R.id.EmailPhoneBox);
        passwordBox = view.findViewById(R.id.PasswordBox);
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

        ImageButton createAccountButton = view.findViewById(R.id.CreateAccountBtn);
        if (createAccountButton != null) {
            createAccountButton.setOnClickListener(v -> handleCreateAccount());
        }

        ImageButton githubButton = view.findViewById(R.id.SignInGithubBtn);
        if (githubButton != null) {
            githubButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "GitHub signup not implemented yet", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_LONG).show();
                        NavbarHelper.navigateToActivity(requireActivity(), home_page.class);
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
        usernameBox = null;
        emailBox = null;
        passwordBox = null;
        checkBoxTermsAndConditions = null;
    }
}