package com.termux.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import com.yalantis.ucrop.UCrop;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;
import com.termux.R;
import com.termux.app.database.SupabaseConfig;
import com.termux.app.database.managers.AuthManager;
import com.termux.app.database.models.SamsaraUser;
import com.termux.app.database.repository.UserRepository;
import com.termux.app.database.utils.ImageUtils;
import com.termux.app.utils.ImagePickerHelper;
import com.termux.app.utils.CircleTransform;
import androidx.core.content.ContextCompat;

public class profile_page extends AppCompatActivity implements ImagePickerHelper.ImagePickerCallback {
    private static final String TAG = "ProfilePage";
    private AuthManager authManager;
    private UserRepository userRepository;
    private EditText usernameBox;
    private EditText emailBox;
    private EditText passwordBox;
    private TextView signInOutText;
    private ImageButton signInOutButton;
    private ImageView profilePictureView;
    private ImagePickerHelper imagePickerHelper;
    private boolean isEditMode = false;
    private String newPasswordForDisplay = null;
    private boolean shouldShowPasswordPopup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        try {
            SupabaseConfig.initialize(this);
            userRepository = UserRepository.getInstance(this);
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
        profilePictureView = findViewById(R.id.imgProfilePic);
        try {
            profilePictureView.setBackgroundResource(R.drawable.account_2);
            float density = getResources().getDisplayMetrics().density;
            int pad = (int) (density * 4f);
            profilePictureView.setPadding(pad, pad, pad, pad);
            profilePictureView.setClipToOutline(false);
            profilePictureView.setScaleX(0.75f);
            profilePictureView.setScaleY(0.75f);
        } catch (Exception ignored) { }
        
        imagePickerHelper = new ImagePickerHelper(this, this);
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

        profilePictureView.setOnClickListener(v -> {
            if (!authManager.isLoggedIn()) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            } else {
                imagePickerHelper.showImagePickerDialog();
            }
        });
    }
    
    private void loadDefaultProfilePicture() {
        profilePictureView.setImageResource(R.drawable.account_2);
    }

    private void loadProfilePicture(String filename) {
        if (TextUtils.isEmpty(filename)) {
            loadDefaultProfilePicture();
            return;
        }
        try {
            String url = userRepository.getProfilePictureUrl(filename);
            float density = getResources().getDisplayMetrics().density;
            int insetPx = Math.max(1, (int) (density * 4f));
            Picasso.get()
                .load(url)
                .resize(ImageUtils.PROFILE_IMAGE_SIZE, ImageUtils.PROFILE_IMAGE_SIZE)
                .centerCrop()
                .transform(new CircleTransform(insetPx))
                .placeholder(R.drawable.account_2)
                .error(R.drawable.account_2)
                .into(profilePictureView);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load profile picture: " + e.getMessage());
            loadDefaultProfilePicture();
        }
    }

    private void loadUserData() {
        if (authManager.isLoggedIn()) {
            SamsaraUser user = authManager.getCurrentUser();
            if (user != null) {
                usernameBox.setText(user.getUsername() != null ? user.getUsername() : "");
                emailBox.setText(user.getEmail() != null ? user.getEmail() : "");
                passwordBox.setText(user.getPasswordHash() != null ? "••••••••" : "");
                String filename = user.getProfilePictureUrl();
                if (!TextUtils.isEmpty(filename)) {
                    loadProfilePicture(filename);
                } else {
                    loadDefaultProfilePicture();
                }
            }
        } else {
            clearFields();
            loadDefaultProfilePicture();
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
            Intent intent = new Intent(this, SignInOut_page.class);
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
            passwordBox.setHint("Enter new password");
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            newPasswordForDisplay = null;
            shouldShowPasswordPopup = false;
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


        if ("••••••••".equals(password)) {
            password = "";
        }

        if (!validateInput(username, email, password)) {
            return;
        }

        if (!TextUtils.isEmpty(password)) {
            newPasswordForDisplay = password;
            shouldShowPasswordPopup = true;
        } else {
            newPasswordForDisplay = null;
            shouldShowPasswordPopup = false;
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
                String updatedPassword = newPasswordForDisplay;
                if (!TextUtils.isEmpty(updatedPassword)) {
                    authManager.signinUser(updatedUser, updatedPassword);
                } else {
                    authManager.signinUser(updatedUser);
                }

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();


                if (shouldShowPasswordPopup && newPasswordForDisplay != null) {
                    showPasswordPopup(newPasswordForDisplay);
                    newPasswordForDisplay = null;
                    shouldShowPasswordPopup = false;
                }

                isEditMode = false;
                loadUserData();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_LONG).show();
                setFieldsEnabled(true);
            }
        });
    }

    private void showPasswordPopup(String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Password")
               .setMessage("Your new password is:\n\n" + password +
                          "\n\nPlease save this password in a secure location. This popup will only appear once.")
               .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
               .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 10000);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imagePickerHelper.handleActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    handleCroppedImage(resultUri);
                } else {
                    Toast.makeText(this, "Failed to retrieve cropped image", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
                Throwable cropError = UCrop.getError(data);
                Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleCroppedImage(Uri imageUri) {
        SamsaraUser currentUser = authManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "User session invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            byte[] imageData = ImageUtils.processImageFromUri(this, imageUri);
            if (imageData == null) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show());
                return;
            }
            if (!ImageUtils.isValidImageSize(imageData)) {
                runOnUiThread(() -> Toast.makeText(this, "Image size must be less than 5MB", Toast.LENGTH_SHORT).show());
                return;
            }
            runOnUiThread(() -> Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show());
            userRepository.uploadProfilePicture(currentUser.getId(), imageData)
                .thenCompose(filename -> {
                    if (filename != null && filename.matches("\\d+_\\d+\\.jpg")) {
                        return userRepository.updateUserProfilePicture(currentUser.getId(), filename)
                            .thenApply(success -> (success != null && success) ? filename : null);
                    }
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                })
                .thenAccept(filename -> runOnUiThread(() -> {
                    if (filename != null && !filename.isEmpty()) {
                        currentUser.setProfilePictureUrl(filename);
                        authManager.signinUser(currentUser);
                        loadProfilePicture(filename);
                        Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                    }
                }))
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error updating profile picture", throwable);
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show());
                    return null;
                });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imagePickerHelper.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onImageSelected(Uri imageUri) {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Invalid image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "profile_crop_" + System.currentTimeMillis() + ".jpg"));

        int targetPx;
        try {
            float density = getResources().getDisplayMetrics().density;
            int basePx = (int) (182f * density);
            int viewMin = 0;
            if (profilePictureView != null) {
                int w = profilePictureView.getWidth();
                int h = profilePictureView.getHeight();
                if (w > 0 && h > 0) viewMin = Math.min(w, h);
            }
            int ref = (viewMin > 0) ? viewMin : basePx;
            targetPx = Math.max(64, Math.min(2048, (int) (ref * 0.75f)));
        } catch (Exception e) {
            targetPx = 384;
        }

        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(85);
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        try {
            float density = getResources().getDisplayMetrics().density;
            int inset = (int) (density * 10f);
            if (targetPx > inset * 2) targetPx = targetPx - (inset * 2);
        } catch (Exception ignored) { }

        UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1, 1)
            .withMaxResultSize(targetPx, targetPx)
            .withOptions(options)
            .start(this);
    }

    @Override
    public void onError(String error) {
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
