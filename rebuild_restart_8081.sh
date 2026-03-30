#!/bin/bash

# Configuration
PROJECT_DIR="/var/www/html/crossword-projects"
PORT=8081
JAR_NAME="crossword-projects-0.0.1-SNAPSHOT.jar"
LOG_FILE="app.log"

echo "==========================================="
echo " Starting Rebuild & Restart for Port $PORT "
echo "==========================================="

# 1. Kill existing process
echo "Step 1: Killing existing process on port $PORT..."
sudo fuser -k $PORT/tcp 2>/dev/null || echo "No process found on port $PORT."

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
    PID=$(sudo lsof -t -i:$PORT)
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
