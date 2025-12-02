#!/bin/bash
# Linux/Mac 脚本 - 运行命令行客户端

echo "Starting PCAgent Chat CLI..."
echo ""

mvn exec:java -Dexec.mainClass="com.pcagent.cli.PCAgentChat" "$@"

