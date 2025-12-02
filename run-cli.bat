@echo off
setlocal enabledelayedexpansion
REM Windows 批处理脚本 - 运行命令行客户端
REM 设置 UTF-8 编码
chcp 65001 >nul 2>&1

echo Starting PCAgent Chat CLI...
echo.

REM 先编译项目
echo Compiling project...
call mvn clean compile -q
if errorlevel 1 (
    echo Error: Compilation failed.
    pause
    exit /b 1
)

REM 复制依赖（如果需要）
if not exist "target\dependency" (
    echo Copying dependencies...
    call mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    if errorlevel 1 (
        echo Warning: Failed to copy dependencies, trying without them...
    )
)

REM 构建类路径
set CLASSPATH=target\classes
if exist "target\dependency" (
    for %%f in (target\dependency\*.jar) do (
        set CLASSPATH=!CLASSPATH!;%%f
    )
)

REM 使用 Java 命令直接运行，避免 Spring Boot 启动问题
REM 设置 JVM 参数：UTF-8 编码，禁用 ANSI 检测（如果需要）
echo Starting CLI client...
echo Connecting to server...
echo.
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "%CLASSPATH%" com.pcagent.cli.PCAgentChat %*

if errorlevel 1 (
    echo.
    echo Error: Failed to start CLI client.
    echo.
    echo Troubleshooting:
    echo 1. Make sure the backend server is running on port 8080
    echo 2. Check if all dependencies are available
    echo 3. Try running: mvn dependency:copy-dependencies
    pause
    exit /b 1
)

pause

