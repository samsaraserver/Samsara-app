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
import java.util.Scanner;
import android.util.Log;

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
        private long lastBytesRead = 0;
        private long lastBytesWritten = 0;
        private long lastUpdateTime = 0;

        void updateStats() {
            // Try multiple approaches for disk stats
            try {
                // First try: Direct IO stats from /proc/self/io
                File ioFile = new File("/proc/self/io");
                if (ioFile.exists() && ioFile.canRead()) {
                    BufferedReader reader = new BufferedReader(new FileReader(ioFile));
                    String line;
                    long bytesRead = 0;
                    long bytesWritten = 0;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("read_bytes:")) {
                            bytesRead = Long.parseLong(line.split(":")[1].trim());
                        } else if (line.startsWith("write_bytes:")) {
                            bytesWritten = Long.parseLong(line.split(":")[1].trim());
                        }
                    }
                    reader.close();

                    long now = System.currentTimeMillis();
                    float duration = (now - lastUpdateTime) / 1000f;

                    if (lastUpdateTime > 0 && duration > 0) {
                        float readSpeed = (bytesRead - lastBytesRead) / duration / (1024 * 1024); // MB/s
                        float writeSpeed = (bytesWritten - lastBytesWritten) / duration / (1024 * 1024);

                        // Only update if we have reasonable values
                        if (readSpeed >= 0 && writeSpeed >= 0) {
                            tvReadWrite2.setText(String.format("R: %.1f MB/s W: %.1f MB/s", readSpeed, writeSpeed));
                        }
                    }

                    lastBytesRead = bytesRead;
                    lastBytesWritten = bytesWritten;
                    lastUpdateTime = now;
                } else {
                    // Second try: Block device stats
                    String[] blockDevices = {"/sys/block/dm-0/stat", "/sys/block/sda/stat",
                                          "/sys/block/mmcblk0/stat", "/dev/block/mmcblk0"};

                    for (String devicePath : blockDevices) {
                        File blockDevice = new File(devicePath);
                        if (blockDevice.exists() && blockDevice.canRead()) {
                            BufferedReader reader = new BufferedReader(new FileReader(blockDevice));
                            String stats = reader.readLine();
                            reader.close();

                            if (stats != null) {
                                String[] fields = stats.trim().split("\\s+");
                                if (fields.length >= 11) {
                                    long sectorsRead = Long.parseLong(fields[2]);
                                    long sectorsWritten = Long.parseLong(fields[6]);

                                    long now = System.currentTimeMillis();
                                    float duration = (now - lastUpdateTime) / 1000f;

                                    if (lastUpdateTime > 0 && duration > 0) {
                                        float readSpeed = (sectorsRead - lastBytesRead) * 512f / duration / (1024 * 1024);
                                        float writeSpeed = (sectorsWritten - lastBytesWritten) * 512f / duration / (1024 * 1024);

                                        if (readSpeed >= 0 && writeSpeed >= 0) {
                                            tvReadWrite2.setText(String.format("R: %.1f MB/s W: %.1f MB/s", readSpeed, writeSpeed));
                                            lastBytesRead = sectorsRead;
                                            lastBytesWritten = sectorsWritten;
                                            lastUpdateTime = now;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading disk stats: " + e.getMessage());
                if (lastUpdateTime == 0) { // Only show -- if we haven't gotten any readings yet
                    tvReadWrite2.setText("R: -- MB/s W: -- MB/s");
                }
            }

            // Update disk utilization
            try {
                StatFs stat = new StatFs(getDataDir().getPath());
                long totalBytes = stat.getTotalBytes();
                long freeBytes = stat.getFreeBytes();
                long usedBytes = totalBytes - freeBytes;
                int utilizationPercentage = (int)((usedBytes * 100.0) / totalBytes);
                tvDiskUtil2.setText(utilizationPercentage + "%");
            } catch (Exception e) {
                e.printStackTrace();
                tvDiskUtil2.setText("--");
            }
        }
    }

    class MonitorSystem {
        private long[] lastCpuStats = new long[4]; // user, nice, system, idle
        private long lastCpuUpdateTime = 0;

        void updateStats() {
            // CPU Usage - using better calculation method
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
                String cpuStats = reader.readLine();
                reader.close();

                if (cpuStats != null && cpuStats.startsWith("cpu")) {
                    String[] values = cpuStats.split("\\s+");
                    long user = Long.parseLong(values[1]);
                    long nice = Long.parseLong(values[2]);
                    long system = Long.parseLong(values[3]);
                    long idle = Long.parseLong(values[4]);

                    if (lastCpuUpdateTime != 0) {
                        long totalDiff = (user + nice + system + idle) -
                                       (lastCpuStats[0] + lastCpuStats[1] + lastCpuStats[2] + lastCpuStats[3]);
                        long activeDiff = (user + nice + system) -
                                        (lastCpuStats[0] + lastCpuStats[1] + lastCpuStats[2]);

                        if (totalDiff > 0) {
                            int cpuUsage = (int)((activeDiff * 100.0) / totalDiff);
                            tvCPU2.setText(cpuUsage + "%");
                        }
                    }

                    lastCpuStats[0] = user;
                    lastCpuStats[1] = nice;
                    lastCpuStats[2] = system;
                    lastCpuStats[3] = idle;
                    lastCpuUpdateTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
                tvCPU2.setText("--");
            }

            // CPU Temperature - improved method with multiple sources
            try {
                String[] tempSources = {
                    "/sys/class/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone1/temp",
                    "/sys/devices/virtual/thermal/thermal_zone0/temp",
                    "/sys/devices/virtual/thermal/thermal_zone1/temp",
                    "/sys/class/hwmon/hwmon0/temp1_input",
                    "/sys/class/hwmon/hwmon1/temp1_input",
                };

                boolean tempFound = false;
                for (String tempPath : tempSources) {
                    File tempFile = new File(tempPath);
                    if (tempFile.exists() && tempFile.canRead()) {
                        BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                        String line = reader.readLine();
                        reader.close();

                        if (line != null && !line.trim().isEmpty()) {
                            try {
                                float temp = Integer.parseInt(line.trim());
                                // Some devices report in millicelsius, others in celsius
                                if (temp > 1000) {
                                    temp = temp / 1000.0f;
                                }
                                if (temp > 0 && temp < 150) { // Sanity check for reasonable temperature
                                    tvCpuTemp2.setText(String.format("%.1f°C", temp));
                                    tempFound = true;
                                    break;
                                }
                            } catch (NumberFormatException nfe) {
                                continue; // Try next source if this one is invalid
                            }
                        }
                    }
                }

                if (!tempFound) {
                    tvCpuTemp2.setText("N/A");
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading CPU temperature: " + e.getMessage());
                tvCpuTemp2.setText("N/A");
            }

            // Memory Usage
            try {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(memoryInfo);
                long totalMem = memoryInfo.totalMem;
                long availMem = memoryInfo.availMem;
                int memoryUsage = (int)((totalMem - availMem) * 100.0 / totalMem);
                tvMemoryUsage2.setText(memoryUsage + "%");
            } catch (Exception e) {
                e.printStackTrace();
                tvMemoryUsage2.setText("--");
            }

            // Thread Count
            tvThreadCount2.setText(String.valueOf(Thread.getAllStackTraces().size()));

            // Handles (File descriptors) - improved reading method
            try {
                File fdDir = new File("/proc/self/fd");
                if (fdDir.exists()) {
                    String[] fds = fdDir.list();
                    if (fds != null) {
                        tvHandles2.setText(String.valueOf(fds.length));
                    }
                } else {
                    // Fallback to reading file-nr
                    BufferedReader reader = new BufferedReader(new FileReader("/proc/sys/fs/file-nr"));
                    String[] parts = reader.readLine().trim().split("\\s+");
                    reader.close();
                    tvHandles2.setText(parts[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tvHandles2.setText("--");
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
