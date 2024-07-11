#!/bin/bash

# Start the application in the background
java -jar /path/to/your/JMXMemoryProfiler.jar &

# Capture the PID
APP_PID=$!

# Output the PID
echo "Application PID: $APP_PID"

# List Java processes using jcmd
jcmd

# Wait for the application to finish
wait $APP_PID