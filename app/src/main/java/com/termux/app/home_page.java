package com.termux.app;

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
            startActivity(intent);
            finish();
        });

        ImageButton configurationBtn = findViewById(R.id.configurationBtn);

        configurationBtn.setOnClickListener(view -> {
            if (this instanceof FragmentActivity) {
                showBiometricPromptForConfig();
            } else {
                showConfigFallbackOptions(this);
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
                        // Navigate to configuration page on successful authentication
                        Intent intent = new Intent(home_page.this, configuration_page.class);
                        startActivity(intent);
                        finish();
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
}
