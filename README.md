# JMXMemoryProfiler

The `JMXMemoryProfiler` application is a Java-based tool designed to monitor and profile memory usage and class loading statistics of a Java Virtual Machine (JVM) remotely using Java Management Extensions (JMX).

## Overview

### Initialization

- The application accepts a JMX URL as a command-line argument to connect to a remote JVM.
- It initializes various MXBeans (`MemoryMXBean`, `ClassLoadingMXBean`, `HotSpotDiagnosticMXBean`) to gather memory and class loading metrics.

### Monitoring

- The application schedules a recurring task using a `ScheduledExecutorService` that runs every 10 seconds to collect and log memory and class loading data.
- Data collected includes heap memory usage, class loading counts, and detailed memory pool statistics.

### Data Logging

- It writes the collected data into a CSV file named `memory_profiler_output.csv`.
- The CSV contains headers for timestamp, heap memory metrics (init, used, committed, max), class loading metrics, and memory pool details.

### Heap Dumps

- Every 60 seconds, the application attempts to generate a heap dump if it has write permissions for the target file path.
- The heap dumps are saved with filenames that include the current timestamp.

### Shutdown Handling

- A shutdown hook ensures that the monitoring process is gracefully terminated, and the CSV writer is properly closed when the application exits.

### Utility Methods

- Utility methods are provided for converting bytes to gigabytes, checking file write permissions, and generating heap dumps.

## Example Usage

To run the application, you would build the jar and specify the JMX connection/port prior to running the Java command below:

```sh
mvn clean package

java -cp target/jvm-heap-profiler-1.0-SNAPSHOT.jar com.example.JMXMemoryProfiler service:jmx:rmi:///jndi/rmi://localhost:xxxxx/jmxrmi
```

Add these java opts into your target application in order to enable jmx remote connections, note I am port forwarding locally in my example below without ssl, you may want to specify a port
```java
-Dproject.features.authenticate-documentation.enabled=false 
-Dcom.sun.management.jmxremote.ssl=false 
-Dcom.sun.management.jmxremote.authenticate=false 
-Dcom.sun.management.jmxremote.port=0 
-Djava.rmi.server.hostname=127.0.0.1 
-Dcom.sun.management.jmxremote.local.only=false
```

# JMXHeapDump

## Overview
`JMXHeapDump` is a Java program that generates a heap dump of the JVM using the `HotSpotDiagnosticMXBean`. This can be useful for diagnosing memory-related issues by analyzing the heap dump file.

## Dependencies
- `java.io.File`
- `java.io.IOException`
- `com.sun.management.HotSpotDiagnosticMXBean`
- `java.lang.management.ManagementFactory`

## Description
The program performs the following steps:
1. **Initialize `HotSpotDiagnosticMXBean`:** Accesses the `HotSpotDiagnosticMXBean` to interact with the JVM.
2. **Set File Path:** Constructs the file path for the heap dump, including a timestamp to ensure unique filenames.
3. **Generate Heap Dump:** Calls the `dumpHeap` method on the `HotSpotDiagnosticMXBean` to create the heap dump file.
4. **Directory and File Permissions:** Ensures the parent directory exists, has write permissions, and sets appropriate permissions on the generated heap dump file.

## Usage
To run this program, execute the `main` method in the `JMXHeapDump` class. The heap dump will be saved in the `~/Downloads/heap_dump` directory with a filename pattern of `heap_dump_<timestamp>.hprof`.

```sh
sudo java -cp target/jvm-heap-profiler-1.0-SNAPSHOT.jar com.example.JMXHeapDump service:jmx:rmi:///jndi/rmi://localhost:xxxxx/jmxrmi
```

## Notes
- Ensure you have the necessary permissions to write to the specified directory.
- Make sure the `com.sun.management.HotSpotDiagnosticMXBean` is available in your JVM implementation.