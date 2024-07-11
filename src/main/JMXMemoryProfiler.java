package main;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ClassLoadingMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.management.HotSpotDiagnosticMXBean;

public class JMXMemoryProfiler {

    private final String jmxUrl;
    private ScheduledExecutorService scheduler;

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

        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                System.out.println("Heap Memory Usage: " + heapMemoryUsage.toString());

                int loadedClassCount = classLoadingMXBean.getLoadedClassCount();
                long totalLoadedClassCount = classLoadingMXBean.getTotalLoadedClassCount();
                long unloadedClassCount = classLoadingMXBean.getUnloadedClassCount();

                System.out.println("Loaded Class Count: " + loadedClassCount);
                System.out.println("Total Loaded Class Count: " + totalLoadedClassCount);
                System.out.println("Unloaded Class Count: " + unloadedClassCount);

                // Trigger heap dump every 60 seconds
                if (System.currentTimeMillis() % 60000 < 10000) {
                    String filePath = "heap_dump_" + System.currentTimeMillis() + ".hprof";
                    System.out.println("Generating heap dump: " + filePath);
                    generateHeapDump(hotspotMxBean, filePath);
                }
            } catch (Exception e) {
                System.err.println("Failed to retrieve memory or class loading data: " + e.getMessage());
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    private void generateHeapDump(HotSpotDiagnosticMXBean hotspotMxBean, String filePath) {
        try {
            hotspotMxBean.dumpHeap(filePath, true);
        } catch (IOException e) {
            System.err.println("Failed to generate heap dump: " + e.getMessage());
        }
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
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
