#!/bin/bash

APP_NAME="quick-deploy.jar"

# 获取 PID
PID=$(/opt/java/jdk-21.0.11+10/bin/jps | grep "$APP_NAME" | awk '{print $1}')
if [ -z "$PID" ]; then
  echo "Process not found: $APP_NAME"
  exit 1
fi

echo "Stopping process PID: $PID ($APP_NAME) ..."

# 优雅停止
kill -15 $PID

# 等待结束
for i in {1..120}; do
  if ! kill -0 $PID 2>/dev/null; then
    echo "Stopped process PID: $PID ($APP_NAME) gracefully"
    exit 0
  fi
  sleep 1
done

# 强制停止
kill -9 $PID
echo "Force killed process PID: $PID ($APP_NAME)"
