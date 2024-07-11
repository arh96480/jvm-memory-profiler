package com.example;

import java.io.File;
import java.io.IOException;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

public class JMXHeapDump {

    private static final String HEAP_DUMP_DIR = "~/Downloads/heap_dump";

    public static void main(String[] args) {
        // Your existing code to connect to JMX and get HotSpotDiagnosticMXBean
        // For demonstration purposes, we'll obtain the HotSpotDiagnosticMXBean directly
        HotSpotDiagnosticMXBean hotspotMxBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);

        String filePath = HEAP_DUMP_DIR + "/heap_dump_" + System.currentTimeMillis() + ".hprof";
        generateHeapDump(hotspotMxBean, filePath);
    }

    private static void generateHeapDump(HotSpotDiagnosticMXBean hotspotMxBean, String filePath) {
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
}
