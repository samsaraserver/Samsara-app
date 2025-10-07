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

public class home_page extends AppCompatActivity {
    private static final String TAG = "HomePage";

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
            startActivity(intent);
            finish();
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
            startActivity(intent);
            finish();
        });

        ImageButton documentationBtn = findViewById(R.id.documentationBtn);

        documentationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, documents_page.class);
            startActivity(intent);
            finish();
        });
    }

    private static void showSignedInAuthenticationOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Authentication Required")
            .setMessage("Please authenticate to access configuration:")
            .setPositiveButton("Use Biometrics", (dialog, which) -> {
                if (activity instanceof FragmentActivity) {
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

    private static void showNonSignedInFallbackOptions(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Configuration Access")
            .setMessage("Choose how to access configuration:")
            .setPositiveButton("Continue to Config", (dialog, which) -> {
                Intent intent = new Intent(activity, configuration_page.class);
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
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
                activity.startActivity(intent);
                if (!(activity instanceof configuration_page)) {
                    activity.finish();
                }
            })
            .setNegativeButton("Online Wiki", (dialog, which) -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.termux.com/wiki/Configuration"));
                activity.startActivity(browserIntent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void showBiometricPromptForConfig() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate to access configuration")
                .setNegativeButtonText("Other Options")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            showOtherOptionsDialog(home_page.this);
                        } else if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                            showPasswordPrompt();
                        } else {
                            showSignedInAuthenticationOptions(home_page.this);
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        showPostAuthenticationOptions(home_page.this);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(home_page.this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
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
                if (validatePassword(password)) {
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

    private boolean validatePassword(String password) {
        AuthManager authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            if (authManager.validatePassword(password)) {
                return true;
            }

            android.content.SharedPreferences prefs = getSharedPreferences("samsara_auth", MODE_PRIVATE);
            String customPassword = prefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (customPassword != null) {
                return password.equals(customPassword);
            }

            android.content.SharedPreferences oldPrefs = getSharedPreferences("SamsaraAuth", MODE_PRIVATE);
            String oldCustomPassword = oldPrefs.getString("user_config_password_" + authManager.getCurrentUser().getUsername(), null);
            if (oldCustomPassword != null) {
                return password.equals(oldCustomPassword);
            }

            return false;
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("samsara_auth", MODE_PRIVATE);
            String storedPassword = prefs.getString("config_password", "admin123");

            if (!password.equals(storedPassword)) {
                android.content.SharedPreferences oldPrefs = getSharedPreferences("SamsaraAuth", MODE_PRIVATE);
                String oldStoredPassword = oldPrefs.getString("config_password", "admin123");
                return password.equals(oldStoredPassword);
            }

            return password.equals(storedPassword);
        }
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
}
