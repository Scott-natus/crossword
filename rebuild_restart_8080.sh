#!/bin/bash

# Configuration
PROJECT_DIR="/var/www/html/java-projects"
PORT=8080
JAR_NAME="board-system-0.0.1-SNAPSHOT.jar"
LOG_FILE="board-debug.log"

echo "==========================================="
echo " Starting Rebuild & Restart for Port $PORT "
echo "==========================================="

# 1. Kill existing process
echo "Step 1: Killing existing process on port $PORT..."
echo "dlrhkeo8453" | sudo -S fuser -k $PORT/tcp 2>/dev/null || echo "No process found on port $PORT."

# 2. Build with Gradle (including tests)
echo "Step 2: Building project with Gradle (clean build)..."
cd $PROJECT_DIR
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "Build Successful!"
    
    # 3. Restart service
    echo "Step 3: Restarting service in background..."
    nohup java -jar build/libs/$JAR_NAME --server.port=$PORT > /dev/null 2>&1 &
    
    echo "Waiting for startup (5s)..."
    sleep 5
    
    # Check if process is alive
    PID=$(echo "dlrhkeo8453" | sudo -S lsof -t -i:$PORT)
    if [ ! -z "$PID" ]; then
        echo "Successfully started (PID: $PID)"
        echo "Tail log for last 10 lines:"
        tail -n 10 $LOG_FILE
    else
        echo "Failed to start service. Check $LOG_FILE for details."
    fi
else
    echo "==========================================="
    echo " !!! ERROR: Build Failed. Aborting. !!! "
    echo "==========================================="
    exit 1
fi

echo "==========================================="
echo " All tasks completed! "
echo "==========================================="
