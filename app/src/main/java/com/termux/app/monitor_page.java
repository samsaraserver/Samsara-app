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

        tvReadWrite2 = findViewById(R.id.tvReadWrite2);
        tvDiskUtil2 = findViewById(R.id.tvDiskUtil2);

        tvCPU2 = findViewById(R.id.tvCPU2);
        tvCpuTemp2 = findViewById(R.id.tvCpuTemp2);
        tvMemoryUsage2 = findViewById(R.id.tvMemoryUsage2);
        tvThreadCount2 = findViewById(R.id.tvThreadCount2);
        tvHandles2 = findViewById(R.id.tvHandles2);

        tvBandwidthUsage2 = findViewById(R.id.tvBandwidthUsage2);
        tvPacketLoss2 = findViewById(R.id.tvPacketLoss2);
        tvNetworkLatency2 = findViewById(R.id.tvNetworkLatency2);
        tvActiveConnections2 = findViewById(R.id.tvActiveConnections2);

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
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
                String cpuLine = reader.readLine();
                reader.close();

                if (cpuLine != null && cpuLine.startsWith("cpu")) {
                    String[] values = cpuLine.split("\\s+");
                    if (values.length >= 8) {
                        long user = Long.parseLong(values[1]);
                        long nice = Long.parseLong(values[2]);
                        long system = Long.parseLong(values[3]);
                        long idle = Long.parseLong(values[4]);
                        long iowait = Long.parseLong(values[5]);
                        long irq = Long.parseLong(values[6]);
                        long softirq = Long.parseLong(values[7]);

                        if (lastCpuUpdateTime > 0) {
                            long userDiff = user - lastCpuStats[0];
                            long niceDiff = nice - lastCpuStats[1];
                            long systemDiff = system - lastCpuStats[2];
                            long idleDiff = idle - lastCpuStats[3];
                            long iowaitDiff = iowait - lastCpuStats[4];
                            long irqDiff = irq - lastCpuStats[5];
                            long softirqDiff = softirq - lastCpuStats[6];

                            long totalCpuTime = userDiff + niceDiff + systemDiff + idleDiff + iowaitDiff + irqDiff + softirqDiff;

                            long activeCpuTime = totalCpuTime - idleDiff - iowaitDiff;

                            if (totalCpuTime > 0) {
                                int cpuUsagePercent = (int) ((activeCpuTime * 100.0) / totalCpuTime);
                                tvCPU2.setText(cpuUsagePercent + "%");
                            }
                        }

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

            tvThreadCount2.setText(String.valueOf(Thread.getAllStackTraces().size()));

            try {
                File fdDir = new File("/proc/self/fd");
                if (fdDir.exists()) {
                    String[] fds = fdDir.list();
                    if (fds != null) {
                        tvHandles2.setText(String.valueOf(fds.length));
                    }
                } else {
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
            long now = System.currentTimeMillis();
            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();

            float duration = (now - lastUpdateTime) / 1000f;
            if (lastUpdateTime > 0 && duration > 0) {
                float downloadSpeed = (rxBytes - lastRxBytes) / duration / 1024;
                float uploadSpeed = (txBytes - lastTxBytes) / duration / 1024;
                tvBandwidthUsage2.setText(String.format(Locale.US, "↓%.1f KB/s ↑%.1f KB/s", downloadSpeed, uploadSpeed));
            }

            lastRxBytes = rxBytes;
            lastTxBytes = txBytes;
            lastUpdateTime = now;

            int connectionCount = getConnectionCount();
            tvActiveConnections2.setText(String.valueOf(connectionCount));

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

        private int getConnectionCount() {
            int systemWide = getSystemWideCount();
            if (systemWide >= 0) return systemWide;

            int total = 0;
            try {
                total += countTcpConnections("/proc/net/tcp");
                total += countTcpConnections("/proc/net/tcp6");
                total += countSimpleSocketTable("/proc/net/udp");
                total += countSimpleSocketTable("/proc/net/udp6");
                total += countSimpleSocketTable("/proc/net/udplite");
                total += countSimpleSocketTable("/proc/net/udplite6");
                total += countSimpleSocketTable("/proc/net/raw");
                total += countSimpleSocketTable("/proc/net/raw6");
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
            }
            try {
                Integer v6 = parseSockstatSum("/proc/net/sockstat6");
                if (v6 != null) {
                    total += v6;
                    foundAny = true;
                }
            } catch (Exception e) {
            }
            try {
                total += countUnixSockets("/proc/net/unix");
                foundAny = true;
            } catch (Exception e) {
            }
            return foundAny ? Math.max(total, 0) : -1;
        }

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
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] fields = line.trim().split("\\s+");
                        if (fields.length > 3) {
                            String state = fields[3];
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

        private int countSimpleSocketTable(String procFile) throws IOException {
            int count = 0;
            File file = new File(procFile);
            if (!file.exists() || !file.canRead()) {
                return count;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
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
