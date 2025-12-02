package com.pcagent.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagent.controller.vo.*;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 产品配置Agent命令行聊天客户端
 * 参考 Claude 的交互设计，提供格式化输出
 */
@Slf4j
public class PCAgentChat {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1";
    private static final String BASE_URL = System.getProperty("pcagent.base.url", 
            System.getenv().getOrDefault("PCAGENT_BASE_URL", DEFAULT_BASE_URL));
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final AnsiFormatter formatter;
    private static final boolean SUPPORTS_ANSI;
    private static final boolean IS_WINDOWS;

    static {
        // 检测操作系统
        IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("windows");
        
        // 检测是否支持 ANSI
        // Windows: 默认不支持（避免乱码），除非使用 Windows Terminal 或 Git Bash
        // Linux/Mac: 通常支持
        boolean supportsAnsi = false;
        if (IS_WINDOWS) {
            // Windows: 检查是否在 Windows Terminal 或 Git Bash 中运行
            String term = System.getenv("TERM");
            String wtSession = System.getenv("WT_SESSION");
            String msystem = System.getenv("MSYSTEM"); // Git Bash
            supportsAnsi = (wtSession != null) || // Windows Terminal
                          (msystem != null && msystem.startsWith("MINGW")); // Git Bash
        } else {
            // Linux/Mac 通常支持 ANSI
            supportsAnsi = true;
        }
        
        SUPPORTS_ANSI = supportsAnsi;
        
        // 设置 UTF-8 编码输出
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 如果设置失败，继续使用默认编码
        }
    }

    public PCAgentChat() {
        this.formatter = new AnsiFormatter(SUPPORTS_ANSI);
    }

    public static void main(String[] args) {
        // 设置控制台编码为 UTF-8（Windows）
        if (IS_WINDOWS) {
            try {
                // 尝试设置控制台代码页为 UTF-8
                new ProcessBuilder("cmd", "/c", "chcp", "65001").inheritIO().start().waitFor();
            } catch (Exception e) {
                // 忽略错误，继续运行
            }
        }
        
        // 检查服务器连接
        System.out.println("Connecting to: " + BASE_URL);
        
        if (IS_WINDOWS && !SUPPORTS_ANSI) {
            System.out.println("提示: 检测到 Windows 环境，已禁用颜色输出以避免乱码");
            System.out.println("      如需彩色输出，请使用 Windows Terminal 或 Git Bash");
            System.out.println();
        }
        
        PCAgentChat chat = new PCAgentChat();
        chat.run();
    }

    public void run() {
        printWelcome();
        
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.print(formatter.bold(formatter.cyan("You: ")));
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                    printMessage(formatter.yellow("Goodbye!"));
                    break;
                }
                
                if ("clear".equalsIgnoreCase(input)) {
                    clearScreen();
                    continue;
                }
                
                // 发送消息
                sendMessage(input);
                
            } catch (Exception e) {
                printError("Error: " + e.getMessage());
                log.error("Error in chat loop", e);
            }
        }
        scanner.close();
    }

    private void sendMessage(String userInput) {
        try {
            // 显示用户消息
            printUserMessage(userInput);
            
            // 创建会话
            SessionVO sessionVO = createSession(userInput);
            if (sessionVO == null) {
                printError("Failed to create session");
                return;
            }
            
            String sessionId = sessionVO.getSessionId();
            
            // 显示初始状态
            displaySession(sessionVO);
            
            // 订阅 SSE 事件（异步）
            subscribeToSession(sessionId);
            
            // 同时使用轮询作为备用方案
            pollSessionUpdates(sessionId);
            
        } catch (Exception e) {
            printError("Failed to send message: " + e.getMessage());
            log.error("Error sending message", e);
        }
    }

    private SessionVO createSession(String userInput) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("user_input", userInput);
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/sessions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            printError("HTTP Error: " + response.statusCode());
            return null;
        }
        
        return objectMapper.readValue(response.body(), SessionVO.class);
    }

    private void subscribeToSession(String sessionId) {
        // SSE 在命令行环境中实现较复杂，使用轮询方式更可靠
        // 这个方法保留作为占位符，实际使用 pollSessionUpdates
    }
    
    private void pollSessionUpdates(String sessionId) {
        new Thread(() -> {
            try {
                int lastProgress = -1;
                String lastStep = null;
                
                while (true) {
                    Thread.sleep(1000); // 每秒轮询一次
                    
                    SessionVO sessionVO = getSession(sessionId);
                    if (sessionVO == null) {
                        continue;
                    }
                    
                    // 检查是否有更新
                    int currentProgress = sessionVO.getProgress() != null ? 
                            sessionVO.getProgress().getCurrent() : 0;
                    String currentStep = sessionVO.getCurrentStep();
                    
                    if (currentProgress != lastProgress || 
                        (currentStep != null && !currentStep.equals(lastStep))) {
                        // 有更新，显示
                        displaySession(sessionVO);
                        lastProgress = currentProgress;
                        lastStep = currentStep;
                        
                        // 如果完成，退出轮询
                        if (sessionVO.getProgress() != null &&
                            sessionVO.getProgress().getCurrent() >= sessionVO.getProgress().getTotal()) {
                            printMessage(formatter.green("\n✓ 配置完成！\n"));
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error polling session updates", e);
            }
        }).start();
    }
    
    private SessionVO getSession(String sessionId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/sessions/" + sessionId))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), SessionVO.class);
            }
        } catch (Exception e) {
            log.debug("Error getting session", e);
        }
        return null;
    }

    private void displaySession(SessionVO sessionVO) {
        if (sessionVO == null) {
            return;
        }
        
        // 显示进度
        if (sessionVO.getProgress() != null) {
            displayProgress(sessionVO.getProgress());
        }
        
        // 显示数据
        if (sessionVO.getDisplayData() != null) {
            displayData(sessionVO.getCurrentStep(), sessionVO.getDisplayData());
        }
    }

    private void displayProgress(ProgressVO progress) {
        int current = progress.getCurrent();
        int total = progress.getTotal();
        String message = progress.getMessage();
        
        // 进度条
        int barWidth = 40;
        int filled = (int) ((double) current / total * barWidth);
        String bar = formatter.green("█".repeat(filled)) + formatter.yellow("░".repeat(barWidth - filled));
        int percentage = (int) ((double) current / total * 100);
        
        // 使用 \r 实现同一行更新
        System.out.print("\r");
        System.out.print(formatter.bold(formatter.blue("Progress: ")));
        System.out.print(bar);
        System.out.print(formatter.bold(" " + percentage + "% (" + current + "/" + total + ")"));
        System.out.flush();
        
        if (message != null && !message.isEmpty()) {
            System.out.println();
            System.out.println(formatter.cyan("  → " + message));
        } else {
            System.out.println();
        }
    }

    private void displayData(String step, Object displayData) {
        if (displayData == null) {
            return;
        }
        
        // 清除之前的进度行
        System.out.print("\r\033[K");
        
        System.out.println();
        System.out.println(formatter.bold(formatter.blue("┌─ " + getStepName(step) + " ──────────────────────────────────────")));
        
        if (step.equals("step1") && displayData instanceof ConfigReqVO) {
            displayConfigReq((ConfigReqVO) displayData);
        } else if (step.equals("step2")) {
            if (displayData instanceof SpecParseResultVO) {
                displaySpecParseResult((SpecParseResultVO) displayData);
            } else if (displayData instanceof ProductSelectionVO) {
                displayProductSelection((ProductSelectionVO) displayData);
            }
        } else if (step.equals("step3") && displayData instanceof ParameterConfigVO) {
            displayParameterConfig((ParameterConfigVO) displayData);
        }
        
        System.out.println(formatter.blue("└─────────────────────────────────────────────────────"));
        System.out.println();
    }

    private void displayConfigReq(ConfigReqVO configReq) {
        System.out.println(formatter.bold("产品系列: ") + formatter.yellow(configReq.getProductSerial()));
        System.out.println(formatter.bold("总套数: ") + formatter.yellow(String.valueOf(configReq.getTotalQuantity())));
        
        if (configReq.getConfigStrategy() != null) {
            System.out.println(formatter.bold("配置策略: ") + formatter.yellow(configReq.getConfigStrategy()));
        }
        
        if (configReq.getSpecReqItems() != null && !configReq.getSpecReqItems().isEmpty()) {
            System.out.println(formatter.bold("\n规格需求项:"));
            for (int i = 0; i < configReq.getSpecReqItems().size(); i++) {
                System.out.println("  " + formatter.cyan((i + 1) + ". ") + configReq.getSpecReqItems().get(i));
            }
        }
    }

    private void displaySpecParseResult(SpecParseResultVO result) {
        if (result.getItems() == null || result.getItems().isEmpty()) {
            return;
        }
        
        System.out.println(formatter.bold("规格解析结果:"));
        System.out.println();
        
        // 表格头部
        System.out.println(formatter.bold("┌────┬──────────────────────────────────────┬────────────────────┐"));
        System.out.println(formatter.bold("│序号│原始规格需求                          │标准规格需求        │"));
        System.out.println(formatter.bold("├────┼──────────────────────────────────────┼────────────────────┤"));
        
        // 表格内容
        for (SpecParseItemVO item : result.getItems()) {
            String index = String.format("%-4s", item.getIndex());
            String original = String.format("%-36s", truncate(item.getOriginalSpec(), 36));
            String std = String.format("%-18s", truncate(item.getStdSpec(), 18));
            System.out.println("│" + formatter.cyan(index) + "│" + original + "│" + formatter.green(std) + "│");
        }
        
        System.out.println(formatter.bold("└────┴──────────────────────────────────────┴────────────────────┘"));
    }

    private void displayProductSelection(ProductSelectionVO selection) {
        if (selection.getSelectedProductCode() != null) {
            System.out.println(formatter.bold("选中产品: ") + 
                    formatter.green(selection.getSelectedProductName() != null ? 
                            selection.getSelectedProductName() : selection.getSelectedProductCode()));
        }
        
        if (selection.getCandidates() != null && !selection.getCandidates().isEmpty()) {
            System.out.println(formatter.bold("\n候选产品排序 (Top3):"));
            System.out.println();
            System.out.println(formatter.bold("┌────┬──────────────────────┬────────┬────────────┐"));
            System.out.println(formatter.bold("│排序│产品名称              │偏离度  │说明        │"));
            System.out.println(formatter.bold("├────┼──────────────────────┼────────┼────────────┤"));
            
            for (ProductSelectionItemVO item : selection.getCandidates()) {
                String rank = String.format("%-4s", item.getRank());
                String name = String.format("%-20s", truncate(
                        item.getProductName() != null ? item.getProductName() : item.getProductCode(), 20));
                String degree = String.format("%-6s", item.getDeviationDegree() + "%");
                String desc = String.format("%-10s", truncate(item.getDescription() != null ? item.getDescription() : "", 10));
                System.out.println("│" + formatter.cyan(rank) + "│" + name + "│" + 
                        formatter.yellow(degree) + "│" + desc + "│");
            }
            
            System.out.println(formatter.bold("└────┴──────────────────────┴────────┴────────────┘"));
        }
        
        if (selection.getDeviationDetails() != null && !selection.getDeviationDetails().isEmpty()) {
            System.out.println(formatter.bold("\n规格偏离度详情:"));
            System.out.println();
            System.out.println(formatter.bold("┌────┬──────────────────────────────────────┬──────────┬────────┬──────┬────────┐"));
            System.out.println(formatter.bold("│序号│原始规格需求                          │标准规格  │产品值  │满足  │偏离    │"));
            System.out.println(formatter.bold("├────┼──────────────────────────────────────┼──────────┼────────┼──────┼────────┤"));
            
            for (SpecDeviationItemVO item : selection.getDeviationDetails()) {
                String index = String.format("%-4s", item.getIndex());
                String original = String.format("%-36s", truncate(item.getOriginalSpecReq(), 36));
                String std = String.format("%-8s", truncate(item.getStdSpecReq(), 8));
                String value = String.format("%-6s", truncate(item.getProductSpecValue(), 6));
                String satisfy = String.format("%-4s", item.getSatisfy() ? "Y" : "N");
                String deviation = String.format("%-6s", truncate(item.getDeviationType(), 6));
                
                String satisfyColor = item.getSatisfy() ? formatter.green(satisfy) : formatter.red(satisfy);
                System.out.println("│" + formatter.cyan(index) + "│" + original + "│" + 
                        formatter.blue(std) + "│" + value + "│" + satisfyColor + "│" + 
                        formatter.yellow(deviation) + "│");
            }
            
            System.out.println(formatter.bold("└────┴──────────────────────────────────────┴──────────┴────────┴──────┴────────┘"));
        }
    }

    private void displayParameterConfig(ParameterConfigVO config) {
        System.out.println(formatter.bold("产品: ") + 
                formatter.green(config.getProductName() != null ? 
                        config.getProductName() : config.getProductCode()));
        
        if (config.getItems() != null && !config.getItems().isEmpty()) {
            System.out.println(formatter.bold("\n参数配置结果:"));
            System.out.println();
            System.out.println(formatter.bold("┌────┬──────────────────────────┬──────────────┬──────────────────┐"));
            System.out.println(formatter.bold("│排序│配置需求                  │参数          │参数值(配置结果)  │"));
            System.out.println(formatter.bold("├────┼──────────────────────────┼──────────────┼──────────────────┤"));
            
            for (ParameterConfigItemVO item : config.getItems()) {
                String index = String.format("%-4s", item.getIndex());
                String req = String.format("%-24s", truncate(item.getConfigReq() != null ? item.getConfigReq() : "", 24));
                String param = String.format("%-12s", truncate(
                        item.getParameterName() != null ? item.getParameterName() : item.getParameterCode(), 12));
                String value = String.format("%-16s", truncate(item.getValue(), 16));
                System.out.println("│" + formatter.cyan(index) + "│" + req + "│" + 
                        formatter.blue(param) + "│" + formatter.green(value) + "│");
            }
            
            System.out.println(formatter.bold("└────┴──────────────────────────┴──────────────┴──────────────────┘"));
        }
        
        if (config.getCheckResult() != null) {
            if (config.getCheckResult().getErrorCode() == 0) {
                System.out.println(formatter.green("\n✓ 校验检查通过，没有错误，符合要求"));
            } else {
                System.out.println(formatter.red("\n✗ 校验检查发现错误: " + 
                        (config.getCheckResult().getErrorMessage() != null ? 
                                config.getCheckResult().getErrorMessage() : "未知错误")));
            }
        }
    }

    private String getStepName(String step) {
        switch (step) {
            case "step1": return "配置需求识别";
            case "step2": return "规格解析/产品选型";
            case "step3": return "参数配置";
            default: return "处理中";
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private void printWelcome() {
        clearScreen();
        System.out.println(formatter.bold(formatter.cyan("╔═══════════════════════════════════════════════════════════╗")));
        System.out.println(formatter.bold(formatter.cyan("║        产品配置Agent - 命令行聊天客户端                  ║")));
        System.out.println(formatter.bold(formatter.cyan("╚═══════════════════════════════════════════════════════════╝")));
        System.out.println();
        System.out.println(formatter.yellow("提示: 输入 'exit' 或 'quit' 退出，输入 'clear' 清屏"));
        System.out.println();
    }

    private void printUserMessage(String message) {
        System.out.println();
        System.out.println(formatter.bold(formatter.cyan("┌─ You ───────────────────────────────────────────────────")));
        System.out.println(message);
        System.out.println(formatter.cyan("└─────────────────────────────────────────────────────────────────"));
    }

    private void printMessage(String message) {
        System.out.println(message);
    }

    private void printError(String message) {
        System.out.println(formatter.red("✗ " + message));
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * ANSI 格式化工具类
     * 如果系统不支持 ANSI，则返回原始文本（无颜色）
     */
    static class AnsiFormatter {
        private final boolean enabled;
        
        // ANSI 颜色码
        private static final String RESET = "\033[0m";
        private static final String BOLD = "\033[1m";
        private static final String RED = "\033[31m";
        private static final String GREEN = "\033[32m";
        private static final String YELLOW = "\033[33m";
        private static final String BLUE = "\033[34m";
        private static final String CYAN = "\033[36m";

        public AnsiFormatter(boolean enabled) {
            this.enabled = enabled;
        }

        public String bold(String text) {
            return enabled ? (BOLD + text + RESET) : text;
        }

        public String red(String text) {
            return enabled ? (RED + text + RESET) : text;
        }

        public String green(String text) {
            return enabled ? (GREEN + text + RESET) : text;
        }

        public String yellow(String text) {
            return enabled ? (YELLOW + text + RESET) : text;
        }

        public String blue(String text) {
            return enabled ? (BLUE + text + RESET) : text;
        }

        public String cyan(String text) {
            return enabled ? (CYAN + text + RESET) : text;
        }
    }
}

