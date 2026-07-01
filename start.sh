#!/bin/bash

# $ mkdir -p /opt/java
# $ cd /opt/java
# $ wget https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux/OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz
# $ tar -zxvf OpenJDK21U-jdk_x64_linux_hotspot_21.0.11_10.tar.gz

nohup /opt/java/jdk-21.0.11+10/bin/java -jar -Xmx2048M -Xms1024M /opt/quick-deploy/quick-deploy.jar --server.port=58080 > /dev/null 2>&1 &
echo "PID: $!"
