mvn clean package

java -cp target/jvm-heap-profiler-1.0-SNAPSHOT.jar com.example.JMXMemoryProfiler service:jmx:rmi:///jndi/rmi://localhost:xxxxx/jmxrmi