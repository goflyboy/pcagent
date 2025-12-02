#!/bin/bash
# Linux/Mac 脚本 - 运行命令行客户端

echo "Starting PCAgent Chat CLI..."
echo ""

# 先编译项目
echo "Compiling project..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "Error: Compilation failed."
    exit 1
fi

# 复制依赖（如果需要）
if [ ! -d "target/dependency" ]; then
    echo "Copying dependencies..."
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
fi

# 构建类路径
CLASSPATH="target/classes"
for jar in target/dependency/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# 使用 Java 命令直接运行，避免 Spring Boot 启动问题
echo "Starting CLI client..."
echo ""
java -cp "$CLASSPATH" com.pcagent.cli.PCAgentChat "$@"

if [ $? -ne 0 ]; then
    echo ""
    echo "Error: Failed to start CLI client."
    exit 1
fi

