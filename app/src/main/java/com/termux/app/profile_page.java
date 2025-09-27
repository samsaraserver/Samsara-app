package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;
import com.termux.app.database.repository.UserRepository;

public class profile_page extends Activity {
    private static final String TAG = "ProfilePage";
    private AuthManager authManager;
    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailBox;
    private EditText passwordBox;
    private TextView signInOutText;
    private ImageButton signInOutButton;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        try {
            SupabaseConfig.initialize(this);
            userRepository = new UserRepository();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Supabase: " + e.getMessage(), e);
        }

        authManager = AuthManager.getInstance(this);
        
        initializeViews();
        setupClickListeners();
        loadUserData();
        updateSignInOutButton();
        
        NavbarHelper.setupNavbar(this);
    }

    private void initializeViews() {
        usernameBox = findViewById(R.id.UserBox);
        emailBox = findViewById(R.id.EmailPhoneBox);
        passwordBox = findViewById(R.id.PasswordBox);
        signInOutText = findViewById(R.id.tvSignInOut);
        signInOutButton = findViewById(R.id.SignInOutBtn);
        
        setFieldsEnabled(false);
    }

    private void setupClickListeners() {
        ImageButton setupBiometricsButton = findViewById(R.id.SetupBiometricsBtn);
        setupBiometricsButton.setOnClickListener(v -> {
            Intent intent = new Intent(profile_page.this, biometrics_page.class);
            startActivity(intent);
        });

        ImageButton editButton = findViewById(R.id.EditInformationBtn);
        editButton.setOnClickListener(v -> toggleEditMode());

        ImageButton saveButton = findViewById(R.id.SaveBtn);
        saveButton.setOnClickListener(v -> saveUserData());

        signInOutButton.setOnClickListener(v -> handleSignInOut());
    }

    private void loadUserData() {
        if (authManager.isLoggedIn()) {
            SamsaraUser currentUser = authManager.getCurrentUser();
            if (currentUser != null) {
                usernameBox.setText(currentUser.getUsername());
                emailBox.setText(currentUser.getEmail());
                passwordBox.setText("••••••••");
            }
        } else {
            usernameBox.setText("");
            emailBox.setText("");
            passwordBox.setText("");
        }
    }

    private void updateSignInOutButton() {
        if (authManager.isLoggedIn()) {
            signInOutText.setText("Log Out");
        } else {
            signInOutText.setText("Sign In");
        }
    }

    private void handleSignInOut() {
        if (authManager.isLoggedIn()) {
            authManager.logoutUser();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            
            clearFields();
            updateSignInOutButton();
            setFieldsEnabled(false);
            isEditMode = false;
        } else {
            Intent intent = new Intent(this, login_page.class);
            startActivity(intent);
        }
    }

    private void toggleEditMode() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in to edit your profile", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditMode = !isEditMode;
        setFieldsEnabled(isEditMode);
        
        if (isEditMode) {
            passwordBox.setText("");
            passwordBox.setHint("Enter new password (optional)");
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            loadUserData();
            Toast.makeText(this, "Edit mode disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in to save changes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEditMode) {
            Toast.makeText(this, "Enable edit mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = usernameBox.getText().toString().trim();
        String email = emailBox.getText().toString().trim();
        String password = passwordBox.getText().toString();

        if (!validateInput(username, email, password)) {
            return;
        }

        SamsaraUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        setFieldsEnabled(false);
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        if (TextUtils.isEmpty(password)) {
            userRepository.updateUserInfo(currentUser.getId(), username, email)
                .thenAccept(success -> handleSaveResult(success, username, email))
                .exceptionally(throwable -> {
                    handleSaveError(throwable);
                    return null;
                });
        } else {
            userRepository.updateUserWithPassword(currentUser.getId(), username, email, password)
                .thenAccept(success -> handleSaveResult(success, username, email))
                .exceptionally(throwable -> {
                    handleSaveError(throwable);
                    return null;
                });
        }
    }

    private void handleSaveResult(boolean success, String username, String email) {
        runOnUiThread(() -> {
            if (success) {
                SamsaraUser updatedUser = authManager.getCurrentUser();
                updatedUser.setUsername(username);
                updatedUser.setEmail(email);
                authManager.loginUser(updatedUser);

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                isEditMode = false;
                loadUserData();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_LONG).show();
                setFieldsEnabled(true);
            }
        });
    }

    private void handleSaveError(Throwable throwable) {
        Log.e(TAG, "Error saving profile", throwable);
        runOnUiThread(() -> {
            setFieldsEnabled(true);
            Toast.makeText(this, "Error saving profile: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
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

        if (!TextUtils.isEmpty(password) && password.length() < 8) {
            passwordBox.setError("Password must be at least 8 characters");
            passwordBox.requestFocus();
            return false;
        }

        return true;
    }

    private void setFieldsEnabled(boolean enabled) {
        usernameBox.setEnabled(enabled);
        emailBox.setEnabled(enabled);
        passwordBox.setEnabled(enabled);
    }

    private void clearFields() {
        usernameBox.setText("");
        emailBox.setText("");
        passwordBox.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        updateSignInOutButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRepository != null) {
            userRepository.shutdown();
        }
    }
}
