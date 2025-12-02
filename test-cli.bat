@echo off
REM 快速测试脚本 - 验证 CLI 能否正常运行
echo Testing CLI client...
echo.

REM 检查编译
if not exist "target\classes\com\pcagent\cli\PCAgentChat.class" (
    echo Compiling...
    call mvn compile -q
    if errorlevel 1 (
        echo Compilation failed!
        pause
        exit /b 1
    )
)

REM 复制依赖
if not exist "target\dependency" (
    echo Copying dependencies...
    call mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
)

REM 构建类路径
set CLASSPATH=target\classes
if exist "target\dependency" (
    for %%f in (target\dependency\*.jar) do (
        set CLASSPATH=!CLASSPATH!;%%f
    )
)

REM 测试运行（只显示帮助信息）
echo.
echo Classpath: %CLASSPATH%
echo.
echo Testing if CLI can start (will exit immediately)...
echo.

REM 检查 Java 版本
java -version
echo.

echo If you see Java version above, try running:
echo   java -cp "%CLASSPATH%" com.pcagent.cli.PCAgentChat
echo.
pause

