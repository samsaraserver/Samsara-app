package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.Html;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import com.termux.R;

public class home_page extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);


        NavbarHelper.setupNavbar(this);

        TextView textViewUnderline = findViewById(R.id.tvIP2);
        ImageButton IPButton = findViewById(R.id.ipBtn);

        IPButton.setOnClickListener(view -> {
            String ipAddress = getDeviceIpAddress();
            textViewUnderline.setText(Html.fromHtml("<u>" + ipAddress + "</u>"));
            });

        ImageButton monitorBtn = findViewById(R.id.monitorBtn);

        monitorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, monitor_page.class);
            startActivity(intent);
            finish();
        });

        ImageButton configurationBtn = findViewById(R.id.configurationBtn);

        configurationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(home_page.this, configuration_page.class);
            startActivity(intent);
            finish();
        });

        ImageButton ProjectBtn = findViewById(R.id.ProjectBtn);

        ProjectBtn.setOnClickListener(view -> {
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

    // Method to get the device's IP address
    private String getDeviceIpAddress() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            // Iterate through all interfaces
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and interfaces that are down
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();

                    // Skip IPv6 addresses and loopback addresses
                    if (addr.isLoopbackAddress() || addr.getHostAddress().contains(":")) {
                        continue;
                    }

                    return addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return "IP not available";
    }
}
