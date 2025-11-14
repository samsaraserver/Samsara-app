package com.termux.app;

import static com.termux.app.NavbarHelper.isUserSignedIn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.termux.R;
import com.termux.app.database.managers.AuthManager;

// Security: Add hashing imports
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class home_page extends AppCompatActivity {
    private static final String TAG = "HomePage";

    private static void showConfigFallbackOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        NavbarHelper.setupNavbar(this);

        TextView ipTextView = findViewById(R.id.tvIP2);
        ImageButton ipButton = findViewById(R.id.ipBtn);

        ipButton.setOnClickListener(view -> {
            if (ipTextView.getText().toString().equals("IP not available") ||
                    ipTextView.getText().toString().isEmpty() ||
                    !ipTextView.getText().toString().contains(".")) {
                String ipAddress = getDeviceIpAddress();
                ipTextView.setText(Html.fromHtml("<u>" + ipAddress + "</u>"));
            } else {
                ipTextView.setText("Show IP");
            }
        });

        ImageButton monitorBtn = findViewById(R.id.monitorBtn);

        monitorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, monitor_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ImageButton configurationBtn = findViewById(R.id.configurationBtn);

        configurationBtn.setOnClickListener(view -> {
            if (isUserSignedIn(this)) {
                showSignedInAuthenticationOptions(this);
            } else {
                showNonSignedInFallbackOptions(this);
            }
        });


        ImageButton projectButton = findViewById(R.id.ProjectBtn);

        projectButton.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, projects_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ImageButton documentationBtn = findViewById(R.id.documentationBtn);

        documentationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, documents_page.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void showBiometricPromptForConfig() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to access configuration")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.d(TAG, "Authentication error: " + errString);
                        showConfigFallbackOptions(home_page.this);
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.d(TAG, "Authentication succeeded");
                        Intent intent = new Intent(home_page.this, configuration_page.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.d(TAG, "Authentication failed");
                        Toast.makeText(home_page.this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private String getDeviceIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();

                    if (addr.isLoopbackAddress() || addr.getHostAddress().contains(":")) {
                        continue;
                    }

                    return addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to read device IP", e);
        }

        return "IP not available";
    }

    private void showPasswordPrompt() {
        android.widget.EditText passwordInput = new android.widget.EditText(this);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Enter password");

        new AlertDialog.Builder(this)
            .setTitle("Password Authentication")
            .setMessage("Enter your password to access configuration:")
            .setView(passwordInput)
            .setPositiveButton("Authenticate", (dialog, which) -> {
                String password = passwordInput.getText().toString();
                // Security: Use secure password validation instead of hardcoded password
                if (validatePasswordSecurely(password)) {
                    showPostAuthenticationOptions(home_page.this);
                } else {
                    Toast.makeText(home_page.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    showSignedInAuthenticationOptions(home_page.this);
                }
            })
            .setNegativeButton("Other Options", (dialog, which) -> {
                showOtherOptionsDialog(home_page.this);
            })
            .setNeutralButton("Cancel", (dialog, which) -> {
                showSignedInAuthenticationOptions(home_page.this);
            })
            .show();
    }

    // Security: Secure password validation method
    private boolean validatePasswordSecurely(String password) {
        AuthManager authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            if (authManager.validatePassword(password)) {
                return true;
            }

            android.content.SharedPreferences prefs = getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);

            String customPassword = prefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (customPassword != null) {
                return securePasswordCompare(password, customPassword);
            }

            android.content.SharedPreferences oldPrefs = getSharedPreferences("SamsaraAuth", android.content.Context.MODE_PRIVATE);
            String oldCustomPassword = oldPrefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (oldCustomPassword != null) {
                return securePasswordCompare(password, oldCustomPassword);
            }

            return false;
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("samsara_auth", android.content.Context.MODE_PRIVATE);

            // Security: Generate secure default password if none exists
            String storedPassword = prefs.getString("config_password", null);
            if (storedPassword == null) {
                storedPassword = generateSecureDefaultPassword();
                prefs.edit().putString("config_password", hashPassword(storedPassword)).apply();
                // Show the generated password to user once
                showGeneratedPasswordDialog(storedPassword);
                return false; // Force user to see the password first
            }

            // Security: Compare against hashed password
            return verifyPassword(password, storedPassword);
        }
    }

    // Security: Generate cryptographically secure default password
    private String generateSecureDefaultPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, 12);
    }

    // Security: Hash password using SHA-256 with salt
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            digest.update(salt);
            byte[] hashedPassword = digest.digest(password.getBytes());

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Security: Verify password against hash
    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);
            if (combined.length < 16) return false;

            byte[] salt = new byte[16];
            byte[] hash = new byte[combined.length - 16];
            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, hash, 0, hash.length);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] testHash = digest.digest(password.getBytes());

            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Security: Constant-time password comparison
    private boolean securePasswordCompare(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // Security: Show generated password dialog
    private void showGeneratedPasswordDialog(String password) {
        new AlertDialog.Builder(this)
            .setTitle("Security Notice")
            .setMessage("A secure configuration password has been generated:\n\n" + password +
                       "\n\nPlease save this password securely. You can change it in settings.")
            .setPositiveButton("I've Saved It", null)
            .setCancelable(false)
            .show();
    }

    private static void showSignedInAuthenticationOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Authentication Required")
            .setMessage("Please authenticate to access configuration:")
            .setPositiveButton("Use Biometrics", (dialog, which) -> {
                if (activity instanceof home_page) {
                    ((home_page) activity).showBiometricPromptForConfig();
                } else {
                    ((home_page) activity).showPasswordPrompt();
                }
            })
            .setNegativeButton("Use Password", (dialog, which) -> {
                ((home_page) activity).showPasswordPrompt();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showPostAuthenticationOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setPositiveButton("Continue to Config", (dialog, which) -> {
                Intent intent = new Intent(activity, configuration_page.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showOtherOptionsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Other Options")
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private static void showNonSignedInFallbackOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setPositiveButton("Continue to Config", (dialog, which) -> {
                Intent intent = new Intent(activity, configuration_page.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }
}
