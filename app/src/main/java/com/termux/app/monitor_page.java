package com.termux.app;

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
import android.os.Build;
import android.os.Environment;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class monitor_page extends AppCompatActivity {
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

    private final Handler handler = new Handler();
    private final int UPDATE_INTERVAL = 1000;
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
        private boolean lastUsedProcIo = false;
        private boolean initialized = false;

        void updateStats() {
            try {
                long now = System.currentTimeMillis();

                File ioFile = new File("/proc/self/io");
                boolean usedProcIo = false;
                long curReadBytes = -1;
                long curWriteBytes = -1;

                if (ioFile.exists() && ioFile.canRead()) {
                    usedProcIo = true;
                    try (BufferedReader reader = new BufferedReader(new FileReader(ioFile))) {
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
                        curReadBytes = bytesRead;
                        curWriteBytes = bytesWritten;
                    }
                } else {
                    String[] blockDevices = {"/sys/block/dm-0/stat", "/sys/block/sda/stat", "/sys/block/mmcblk0/stat"};
                    for (String devicePath : blockDevices) {
                        File blockDevice = new File(devicePath);
                        if (blockDevice.exists() && blockDevice.canRead()) {
                            try (BufferedReader reader = new BufferedReader(new FileReader(blockDevice))) {
                                String stats = reader.readLine();
                                if (stats != null) {
                                    String[] fields = stats.trim().split("\\s+");
                                    if (fields.length >= 11) {
                                        long sectorsRead = Long.parseLong(fields[2]);
                                        long sectorsWritten = Long.parseLong(fields[6]);
                                        curReadBytes = (long) (sectorsRead * 512L);
                                        curWriteBytes = (long) (sectorsWritten * 512L);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (curReadBytes >= 0 && curWriteBytes >= 0) {
                    if (!initialized || usedProcIo != lastUsedProcIo) {
                        initialized = true;
                        lastUsedProcIo = usedProcIo;
                        lastBytesRead = curReadBytes;
                        lastBytesWritten = curWriteBytes;
                        lastUpdateTime = now;
                    } else {
                        float duration = (now - lastUpdateTime) / 1000f;
                        if (duration > 0) {
                            float readSpeed = (curReadBytes - lastBytesRead) / duration / (1024f * 1024f);
                            float writeSpeed = (curWriteBytes - lastBytesWritten) / duration / (1024f * 1024f);
                            if (readSpeed >= 0 && writeSpeed >= 0) {
                                tvReadWrite2.setText(String.format(Locale.US, "R: %.1f MB/s W: %.1f MB/s", readSpeed, writeSpeed));
                            }
                            lastBytesRead = curReadBytes;
                            lastBytesWritten = curWriteBytes;
                            lastUpdateTime = now;
                        }
                    }
                } else if (!initialized) {
                    tvReadWrite2.setText("R: -- MB/s W: -- MB/s");
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading disk stats: " + e.getMessage());
                if (lastUpdateTime == 0) {
                    tvReadWrite2.setText("R: -- MB/s W: -- MB/s");
                }
            }

            // Update disk utilization
            try {
                String dataPath;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dataPath = getDataDir().getAbsolutePath();
                } else {
                    File dataDir = Environment.getDataDirectory();
                    dataPath = (dataDir != null ? dataDir.getAbsolutePath() : getFilesDir().getAbsolutePath());
                }

                StatFs stat = new StatFs(dataPath);

                long totalBytes = stat.getTotalBytes();
                long freeBytes = stat.getFreeBytes();

                if (totalBytes > 0) {
                    long usedBytes = totalBytes - freeBytes;
                    int utilizationPercentage = (int) ((usedBytes * 100.0) / totalBytes);
                    tvDiskUtil2.setText(utilizationPercentage + "%");
                } else {
                    tvDiskUtil2.setText("--");
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading disk utilization: " + e.getMessage());
                tvDiskUtil2.setText("--");
            }
        }
    }

    class MonitorSystem {
        private final long[] lastCpuStats = new long[10];
        private long lastCpuUpdateTime = 0;

        void updateStats() {
            // CPU usage calculation
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
                String cpuLine = reader.readLine();
                reader.close();

                if (cpuLine != null && cpuLine.startsWith("cpu")) {
                    // Split the CPU stats line into components
                    String[] values = cpuLine.split("\\s+");
                    if (values.length >= 8) {
                        // Extract CPU time values
                        long user = Long.parseLong(values[1]);
                        long nice = Long.parseLong(values[2]);
                        long system = Long.parseLong(values[3]);
                        long idle = Long.parseLong(values[4]);
                        long iowait = Long.parseLong(values[5]);
                        long irq = Long.parseLong(values[6]);
                        long softirq = Long.parseLong(values[7]);

                        // Calculate CPU usage with previous values
                        if (lastCpuUpdateTime > 0) {
                            long userDiff = user - lastCpuStats[0];
                            long niceDiff = nice - lastCpuStats[1];
                            long systemDiff = system - lastCpuStats[2];
                            long idleDiff = idle - lastCpuStats[3];
                            long iowaitDiff = iowait - lastCpuStats[4];
                            long irqDiff = irq - lastCpuStats[5];
                            long softirqDiff = softirq - lastCpuStats[6];

                            // Total time spent by CPU
                            long totalCpuTime = userDiff + niceDiff + systemDiff + idleDiff + iowaitDiff + irqDiff + softirqDiff;

                            // Active time
                            long activeCpuTime = totalCpuTime - idleDiff - iowaitDiff;

                            if (totalCpuTime > 0) {
                                int cpuUsagePercent = (int) ((activeCpuTime * 100.0) / totalCpuTime);
                                tvCPU2.setText(cpuUsagePercent + "%");
                                Log.d("MonitorPage", "CPU Usage: " + cpuUsagePercent + "%");
                            }
                        }

                        // Save current values for next calculation
                        lastCpuStats[0] = user;
                        lastCpuStats[1] = nice;
                        lastCpuStats[2] = system;
                        lastCpuStats[3] = idle;
                        lastCpuStats[4] = iowait;
                        lastCpuStats[5] = irq;
                        lastCpuStats[6] = softirq;
                        lastCpuUpdateTime = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading CPU stats: " + e.getMessage());
                tvCPU2.setText("--");
            }

            try {
                // Try several potential temperature sources in order
                String[] tempSources = {
                    "/sys/class/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone1/temp",
                    "/sys/class/thermal/thermal_zone2/temp",
                    "/sys/class/thermal/thermal_zone3/temp",
                    "/sys/devices/virtual/thermal/thermal_zone0/temp",
                    "/sys/devices/virtual/thermal/thermal_zone1/temp",
                    "/sys/class/hwmon/hwmon0/temp1_input",
                    "/sys/class/hwmon/hwmon1/temp1_input",
                    "/sys/class/hwmon/hwmon0/device/temp1_input",
                    "/sys/class/hwmon/hwmon1/device/temp1_input"
                };

                boolean tempFound = false;
                for (String tempPath : tempSources) {
                    File tempFile = new File(tempPath);
                    if (tempFile.exists() && tempFile.canRead()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                            String line = reader.readLine();

                            if (line != null && !line.trim().isEmpty()) {
                                try {
                                    float temp = Float.parseFloat(line.trim());

                                    if (temp > 1000) {
                                        temp = temp / 1000.0f;
                                    }
                                    if (temp > 0 && temp < 150) {
                                        final float finalTemp = temp;
                                        handler.post(() -> tvCpuTemp2.setText(String.format(Locale.US, "%.1f°C", finalTemp)));
                                        Log.d("MonitorPage", "CPU Temp: " + temp + "°C from " + tempPath);
                                        tempFound = true;
                                        break;
                                    }
                                } catch (NumberFormatException nfe) {
                                    Log.e("MonitorPage", "Invalid temperature format in " + tempPath + ": " + line);
                                }
                            }
                        } catch (IOException e) {
                            Log.e("MonitorPage", "Error reading temp file " + tempPath + ": " + e.getMessage());
                        }
                    }
                }

                if (!tempFound) {
                    // Alternative method: try to read from the shell
                    try {
                        Process process = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone*/temp");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null && !tempFound) {
                            try {
                                float temp = Float.parseFloat(line.trim());
                                if (temp > 1000) {
                                    temp = temp / 1000.0f;
                                }
                                if (temp > 0 && temp < 150) {
                                    final float finalTemp = temp;
                                    handler.post(() -> tvCpuTemp2.setText(String.format(Locale.US, "%.1f°C", finalTemp)));
                                    tempFound = true;
                                    break;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        reader.close();
                    } catch (IOException e) {
                        Log.e("MonitorPage", "Error reading temperature via shell: " + e.getMessage());
                    }
                }

                if (!tempFound) {
                    handler.post(() -> tvCpuTemp2.setText("N/A"));
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading CPU temperature: " + e.getMessage());
                handler.post(() -> tvCpuTemp2.setText("N/A"));
            }

            // Memory Usage
            try {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(memoryInfo);
                long totalMem = memoryInfo.totalMem;
                long availMem = memoryInfo.availMem;
                int memoryUsage = (int) ((totalMem - availMem) * 100.0 / totalMem);
                tvMemoryUsage2.setText(memoryUsage + "%");
            } catch (Exception e) {
                Log.e("MonitorPage", "Error reading memory usage: " + e.getMessage());
                tvMemoryUsage2.setText("--");
            }

            // Thread Count
            tvThreadCount2.setText(String.valueOf(Thread.getAllStackTraces().size()));

            // Handles
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
                Log.e("MonitorPage", "Error reading handles: " + e.getMessage());
                tvHandles2.setText("--");
            }
        }
    }

    class MonitorNetwork {
        private long lastRxBytes = 0;
        private long lastTxBytes = 0;
        private long lastUpdateTime = 0;
        private final String pingTarget = "1.1.1.1";
        private String lastKnownLatency = "--";
        private String lastKnownPacketLoss = "0%";
        private long lastMetricsCheckMs = 0;
        private static final long METRICS_INTERVAL_MS = 5000;

        void updateStats() {
            // Bandwidth Usage
            long now = System.currentTimeMillis();
            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();

            float duration = (now - lastUpdateTime) / 1000f;
            if (lastUpdateTime > 0 && duration > 0) {
                float downloadSpeed = (rxBytes - lastRxBytes) / duration / 1024; // KB/s
                float uploadSpeed = (txBytes - lastTxBytes) / duration / 1024;
                tvBandwidthUsage2.setText(String.format(Locale.US, "↓%.1f KB/s ↑%.1f KB/s", downloadSpeed, uploadSpeed));
            }

            lastRxBytes = rxBytes;
            lastTxBytes = txBytes;
            lastUpdateTime = now;

            // Network statistics
            int connectionCount = getConnectionCount();
            tvActiveConnections2.setText(String.valueOf(connectionCount));

            // Measure network latency and packet loss (throttled)
            if (now - lastMetricsCheckMs >= METRICS_INTERVAL_MS) {
                lastMetricsCheckMs = now;
                measureNetworkMetrics();
            }
        }

        private void measureNetworkMetrics() {
            new Thread(() -> {
                try {
                    Process process = Runtime.getRuntime().exec("/system/bin/ping -c 5 -w 3 " + pingTarget);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    boolean foundLatency = false;

                    while ((line = reader.readLine()) != null) {
                        if (line.contains("time=") && !foundLatency) {
                            try {
                                final String latencyText = line.substring(line.indexOf("time=") + 5, line.indexOf(" ms"));
                                float latencyValue = Float.parseFloat(latencyText);
                                lastKnownLatency = String.format(Locale.US, "%.1f", latencyValue);
                                foundLatency = true;
                                handler.post(() -> tvNetworkLatency2.setText(lastKnownLatency + " ms"));
                            } catch (Exception e) {
                                Log.e("MonitorPage", "Error parsing latency: " + e.getMessage());
                            }
                        }

                        if (line.contains("packet loss")) {
                            try {
                                int percentIndex = line.indexOf("%");
                                if (percentIndex > 0) {
                                    int startIndex = percentIndex - 1;
                                    while (startIndex >= 0 && (Character.isDigit(line.charAt(startIndex)) || line.charAt(startIndex) == '.')) {
                                        startIndex--;
                                    }
                                    startIndex++;

                                    String lossPercentage = line.substring(startIndex, percentIndex + 1);
                                    lastKnownPacketLoss = lossPercentage;
                                    handler.post(() -> tvPacketLoss2.setText(lossPercentage));
                                }
                            } catch (Exception e) {
                                Log.e("MonitorPage", "Error parsing packet loss: " + e.getMessage());
                            }
                        }
                    }

                    reader.close();

                    if (!foundLatency) {
                        handler.post(() -> tvNetworkLatency2.setText(lastKnownLatency + " ms"));
                    }

                    int exitValue = process.waitFor();
                    if (exitValue != 0 && !foundLatency) {
                        handler.post(() -> {
                            tvNetworkLatency2.setText(lastKnownLatency + " ms");
                            tvPacketLoss2.setText(lastKnownPacketLoss);
                        });
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e("MonitorPage", "Network metrics error: " + e.getMessage());
                    handler.post(() -> {
                        tvNetworkLatency2.setText(lastKnownLatency + " ms");
                        tvPacketLoss2.setText(lastKnownPacketLoss);
                    });
                }
            }).start();
        }

        // Unified connection count across protocols
        private int getConnectionCount() {
            int systemWide = getSystemWideCount();
            if (systemWide >= 0) return systemWide;

            int total = 0;
            try {
                // TCP (exclude LISTEN)
                total += countTcpConnections("/proc/net/tcp");
                total += countTcpConnections("/proc/net/tcp6");
                // UDP
                total += countSimpleSocketTable("/proc/net/udp");
                total += countSimpleSocketTable("/proc/net/udp6");
                // UDPLITE
                total += countSimpleSocketTable("/proc/net/udplite");
                total += countSimpleSocketTable("/proc/net/udplite6");
                // RAW
                total += countSimpleSocketTable("/proc/net/raw");
                total += countSimpleSocketTable("/proc/net/raw6");
                // UNIX
                total += countUnixSockets("/proc/net/unix");
            } catch (IOException e) {
                Log.e("MonitorPage", "Error counting connections: " + e.getMessage());
            }
            return Math.max(total, 0);
        }

        private int getSystemWideCount() {
            int total = 0;
            boolean foundAny = false;
            try {
                Integer v4 = parseSockstatSum("/proc/net/sockstat");
                if (v4 != null) {
                    total += v4;
                    foundAny = true;
                }
            } catch (Exception e) {
                Log.d("MonitorPage", "sockstat v4 not available: " + e.getMessage());
            }
            try {
                Integer v6 = parseSockstatSum("/proc/net/sockstat6");
                if (v6 != null) {
                    total += v6;
                    foundAny = true;
                }
            } catch (Exception e) {
                Log.d("MonitorPage", "sockstat v6 not available: " + e.getMessage());
            }
            // Try to include UNIX domain sockets as well
            try {
                total += countUnixSockets("/proc/net/unix");
                foundAny = true; // if readable, we consider we have some data
            } catch (Exception e) {
                Log.d("MonitorPage", "unix sockets not available: " + e.getMessage());
            }
            return foundAny ? Math.max(total, 0) : -1;
        }

        // Sum of inuse counts for TCP, UDP, UDPLITE, RAW from a sockstat-like file
        private Integer parseSockstatSum(String path) throws IOException {
            File f = new File(path);
            if (!f.exists() || !f.canRead()) return null;
            int sum = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("TCP:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("TCP6:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("UDP:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("UDP6:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("UDPLITE:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("UDPLITE6:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("RAW:")) {
                        sum += extractKeyedInt(line, "inuse");
                    } else if (line.startsWith("RAW6:")) {
                        sum += extractKeyedInt(line, "inuse");
                    }
                }
            }
            return sum;
        }

        private int extractKeyedInt(String line, String key) {
            try {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals(key)) {
                        return Integer.parseInt(parts[i + 1]);
                    }
                }
            } catch (Exception ignored) {}
            return 0;
        }

        private int countTcpConnections(String procFile) throws IOException {
            int count = 0;
            File file = new File(procFile);
            if (!file.exists() || !file.canRead()) {
                return count;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                // Skip header
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] fields = line.trim().split("\\s+");
                        if (fields.length > 3) {
                            String state = fields[3];
                            // Exclude LISTEN (0A), count everything else as active or in-transition
                            if (!"0A".equalsIgnoreCase(state)) {
                                count++;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MonitorPage", "Error parsing TCP connection: " + e.getMessage());
                    }
                }
            }
            return count;
        }

        // For tables like udp, udp6, raw, raw6, udplite, udplite6: count non-header lines
        private int countSimpleSocketTable(String procFile) throws IOException {
            int count = 0;
            File file = new File(procFile);
            if (!file.exists() || !file.canRead()) {
                return count;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                // Skip header
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    count++;
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error parsing table " + procFile + ": " + e.getMessage());
            }
            return count;
        }

        private int countUnixSockets(String procFile) throws IOException {
            int count = 0;
            File file = new File(procFile);
            if (!file.exists() || !file.canRead()) {
                return 0;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                // Skip header
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    count++;
                }
            } catch (Exception e) {
                Log.e("MonitorPage", "Error parsing UNIX sockets: " + e.getMessage());
            }
            return count;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
