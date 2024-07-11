#!/bin/bash

# Start the application in the background
java -cp target/jvm-heap-profiler-1.0-SNAPSHOT.jar JMXMemoryProfiler service:jmx:rmi:///jndi/rmi://localhost:xxxxx/jmxrmi

# Capture the PID
APP_PID=$!

# Output the PID
echo "Application PID: $APP_PID"

# List Java processes using jcmd
jcmd

# Wait for the application to finish
wait $APP_PID