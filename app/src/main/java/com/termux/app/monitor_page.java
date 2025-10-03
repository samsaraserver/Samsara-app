package com.termux.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import com.termux.R;
import android.os.StatFs;
import java.io.*;
import android.net.TrafficStats;
import android.app.ActivityManager;
import android.content.Context;

public class monitor_page extends Activity {
    private TextView tvReadWrite2;
    private TextView tvDiskUtil2;
    private TextView tvCPU2;
    private TextView tvCpuTemp2;
    private TextView tvMemoryUsage2;
    private TextView tvThreadCount2;
    private TextView tvHandles2;
    private TextView tvBandwidthUsage2;
    private TextView tvPacketLoss2;
    private TextView tvNetworkLatency2;
    private TextView tvActiveConnections2;

    private Handler handler = new Handler();
    private final int UPDATE_INTERVAL = 1000; // Update every second
    private MonitorDisk diskMonitor;
    private MonitorSystem systemMonitor;
    private MonitorNetwork networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_page);
        NavbarHelper.setupNavbar(this);

        //track read write and disk utilization
        tvReadWrite2 = findViewById(R.id.tvReadWrite2);
        tvDiskUtil2 = findViewById(R.id.tvDiskUtil2);

        //track cpu, memory, threads, handles
        tvCPU2 = findViewById(R.id.tvCPU2);
        tvCpuTemp2 = findViewById(R.id.tvCpuTemp2);
        tvMemoryUsage2 = findViewById(R.id.tvMemoryUsage2);
        tvThreadCount2 = findViewById(R.id.tvThreadCount2);
        tvHandles2 = findViewById(R.id.tvHandles2);

        //track network bandwidth, packet loss, latency, active connections
        tvBandwidthUsage2 = findViewById(R.id.tvBandwidthUsage2);
        tvPacketLoss2 = findViewById(R.id.tvPacketLoss2);
        tvNetworkLatency2 = findViewById(R.id.tvNetworkLatency2);
        tvActiveConnections2 = findViewById(R.id.tvActiveConnections2);

        // Initialize monitors
        diskMonitor = new MonitorDisk();
        systemMonitor = new MonitorSystem();
        networkMonitor = new MonitorNetwork();

        // Start periodic updates
        startMonitoring();
    }

    private void startMonitoring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateAllStats();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    private void updateAllStats() {
        diskMonitor.updateStats();
        systemMonitor.updateStats();
        networkMonitor.updateStats();
    }

    class MonitorDisk {
        private long lastReadBytes = 0;
        private long lastWriteBytes = 0;
        private long lastUpdateTime = 0;

        void updateStats() {
            // Update disk read/write speeds
            try {
                File diskStats = new File("/proc/diskstats");
                if (diskStats.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(diskStats));
                    String line;
                    long totalRead = 0;
                    long totalWrite = 0;

                    while ((line = reader.readLine()) != null) {
                        String[] stats = line.trim().split("\\s+");
                        if (stats.length >= 14) {
                            totalRead += Long.parseLong(stats[5]) * 512;  // sectors * 512 bytes
                            totalWrite += Long.parseLong(stats[9]) * 512;
                        }
                    }
                    reader.close();

                    long now = System.currentTimeMillis();
                    float duration = (now - lastUpdateTime) / 1000f;
                    if (lastUpdateTime > 0 && duration > 0) {
                        float readSpeed = (totalRead - lastReadBytes) / duration / 1024 / 1024;
                        float writeSpeed = (totalWrite - lastWriteBytes) / duration / 1024 / 1024;
                        tvReadWrite2.setText(String.format("R: %.1f MB/s W: %.1f MB/s", readSpeed, writeSpeed));
                    }

                    lastReadBytes = totalRead;
                    lastWriteBytes = totalWrite;
                    lastUpdateTime = now;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Update disk utilization
            StatFs stat = new StatFs(getDataDir().getPath());
            long totalBytes = stat.getTotalBytes();
            long freeBytes = stat.getFreeBytes();
            long usedBytes = totalBytes - freeBytes;
            int utilizationPercentage = (int)((usedBytes * 100.0) / totalBytes);
            tvDiskUtil2.setText(utilizationPercentage + "%");
        }
    }

    class MonitorSystem {
        void updateStats() {
            // CPU Usage
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
                String cpuStats = reader.readLine();
                if (cpuStats != null && cpuStats.startsWith("cpu")) {
                    String[] values = cpuStats.split("\\s+");
                    long total = 0;
                    long idle = Long.parseLong(values[4]);
                    for (int i = 1; i < values.length; i++) {
                        total += Long.parseLong(values[i]);
                    }
                    int cpuUsage = (int)(100.0 * (1 - idle / (double)total));
                    tvCPU2.setText(cpuUsage + "%");
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // CPU Temperature
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
                float temp = Integer.parseInt(reader.readLine()) / 1000.0f;
                tvCpuTemp2.setText(String.format("%.1f°C", temp));
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Memory Usage
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(memoryInfo);
            long totalMem = memoryInfo.totalMem;
            long availMem = memoryInfo.availMem;
            int memoryUsage = (int)((totalMem - availMem) * 100.0 / totalMem);
            tvMemoryUsage2.setText(memoryUsage + "%");

            // Thread Count
            tvThreadCount2.setText(String.valueOf(Thread.getAllStackTraces().size()));

            // Handles (File descriptors)
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/sys/fs/file-nr"));
                String[] parts = reader.readLine().split("\t");
                tvHandles2.setText(parts[0]);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class MonitorNetwork {
        private long lastRxBytes = 0;
        private long lastTxBytes = 0;
        private long lastUpdateTime = 0;

        void updateStats() {
            // Bandwidth Usage
            long now = System.currentTimeMillis();
            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();

            float duration = (now - lastUpdateTime) / 1000f;
            if (lastUpdateTime > 0 && duration > 0) {
                float downloadSpeed = (rxBytes - lastRxBytes) / duration / 1024; // KB/s
                float uploadSpeed = (txBytes - lastTxBytes) / duration / 1024;
                tvBandwidthUsage2.setText(String.format("↓%.1f KB/s ↑%.1f KB/s", downloadSpeed, uploadSpeed));
            }

            lastRxBytes = rxBytes;
            lastTxBytes = txBytes;
            lastUpdateTime = now;

            // Network statistics
            int activeConnections = getActiveConnections();
            tvActiveConnections2.setText(String.valueOf(activeConnections));

            // Example packet loss and latency (you might want to implement actual ping tests)
            tvPacketLoss2.setText("0%");
            tvNetworkLatency2.setText("--");
        }

        private int getActiveConnections() {
            int connections = 0;
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/net/tcp"));
                while (reader.readLine() != null) {
                    connections++;
                }
                reader.close();
                return Math.max(0, connections - 1); // Subtract header line
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
