@echo off
REM Windows 批处理脚本 - 运行命令行客户端
echo Starting PCAgent Chat CLI...
echo.
mvn exec:java -Dexec.mainClass="com.pcagent.cli.PCAgentChat" -Dexec.args="%*"
pause

