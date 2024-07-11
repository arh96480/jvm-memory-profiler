package com.example;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.management.HotSpotDiagnosticMXBean;

public class JMXMemoryProfiler {

    private static final String HEAP_DUMP_DIR = "~/Downloads/heap_dump";
    private final String jmxUrl;
    private ScheduledExecutorService scheduler;
    private FileWriter csvWriter;

    public JMXMemoryProfiler(String jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

    public void startMonitoring() throws Exception {
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        MemoryMXBean memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
        HotSpotDiagnosticMXBean hotspotMxBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);

        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class);

        // Initialize CSV writer
        csvWriter = new FileWriter("memory_profiler_output.csv");
        writeCsvHeader();

        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                int loadedClassCount = classLoadingMXBean.getLoadedClassCount();
                long totalLoadedClassCount = classLoadingMXBean.getTotalLoadedClassCount();
                long unloadedClassCount = classLoadingMXBean.getUnloadedClassCount();

                StringBuilder csvLine = new StringBuilder();
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                csvLine.append(timestamp).append(",");

                csvLine.append(bytesToGigabytes(heapMemoryUsage.getInit())).append(",");
                csvLine.append(bytesToGigabytes(heapMemoryUsage.getUsed())).append(",");
                csvLine.append(bytesToGigabytes(heapMemoryUsage.getCommitted())).append(",");
                csvLine.append(bytesToGigabytes(heapMemoryUsage.getMax())).append(",");

                csvLine.append(loadedClassCount).append(",");
                csvLine.append(totalLoadedClassCount).append(",");
                csvLine.append(unloadedClassCount).append(",");

                for (MemoryPoolMXBean pool : memoryPools) {
                    MemoryUsage usage = pool.getUsage();
                    csvLine.append(pool.getName()).append(":")
                            .append("init=").append(bytesToGigabytes(usage.getInit())).append("gb,")
                            .append("used=").append(bytesToGigabytes(usage.getUsed())).append("gb,")
                            .append("committed=").append(bytesToGigabytes(usage.getCommitted())).append("gb,")
                            .append("max=").append(bytesToGigabytes(usage.getMax())).append("gb;");
                }

                // Write to CSV
                csvWriter.append(csvLine.toString()).append("\n");
                csvWriter.flush();

                // Trigger heap dump every 60 seconds
                if (System.currentTimeMillis() % 60000 < 10000) {
                    String filePath = HEAP_DUMP_DIR + "/heap_dump_" + System.currentTimeMillis() + ".hprof";
                    generateHeapDump(hotspotMxBean, filePath);
                }
            } catch (Exception e) {
                System.err.println("Failed to retrieve memory or class loading data: " + e.getMessage());
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    private void writeCsvHeader() throws IOException {
        csvWriter.append("Timestamp,Heap Init (GB),Heap Used (GB),Heap Committed (GB),Heap Max (GB),");
        csvWriter.append("Loaded Class Count,Total Loaded Class Count,Unloaded Class Count,Memory Pools\n");
        csvWriter.flush();
    }

    private void generateHeapDump(HotSpotDiagnosticMXBean hotspotMxBean, String filePath) {
        try {
            System.out.println("Attempting to generate heap dump at: " + filePath);
            File file = new File(filePath);

            // Check if parent directory exists and create it if necessary
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
                } else {
                    System.out.println("Created directory: " + parentDir.getAbsolutePath());
                }
            }

            // Ensure the directory has write permissions
            if (!parentDir.setWritable(true)) {
                throw new IOException("Failed to set write permission for directory: " + parentDir.getAbsolutePath());
            }

            // Generate heap dump
            hotspotMxBean.dumpHeap(filePath, true);
            System.out.println("Heap dump successfully created at: " + filePath);

            // Set file permissions to read, write, and execute for all users
            if (!file.setReadable(true, false) || !file.setWritable(true, false) || !file.setExecutable(true, false)) {
                throw new IOException("Failed to set permissions for file: " + file.getAbsolutePath());
            }
            System.out.println("Permissions set to read, write, and execute for all users on file: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to generate heap dump: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double bytesToGigabytes(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        try {
            if (csvWriter != null) {
                csvWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close CSV writer: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java JMXMemoryProfiler <JMX_URL>");
            System.exit(1);
        }

        String jmxUrl = args[0];
        JMXMemoryProfiler profiler = new JMXMemoryProfiler(jmxUrl);

        try {
            profiler.startMonitoring();
            Runtime.getRuntime().addShutdownHook(new Thread(profiler::stopMonitoring));
        } catch (Exception e) {
            System.err.println("Failed to start memory profiling: " + e.getMessage());
        }
    }
}
