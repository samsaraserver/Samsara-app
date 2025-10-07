package com.termux.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

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
            String ipAddress = getDeviceIpAddress();
            ipTextView.setText(Html.fromHtml("<u>" + ipAddress + "</u>"));
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
