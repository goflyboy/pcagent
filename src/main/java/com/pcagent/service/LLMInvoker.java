package com.pcagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * LLM调用服务
 * 支持deepseek和Qinwen两个模型
 * 注意：由于Spring AI依赖问题，当前使用简单解析实现
 */
@Slf4j
@Service
public class LLMInvoker {
    @Value("${spring.ai.provider:openai}")
    private String provider;

    private final ChatModel chatModel;
    private final Configuration freeMarkerConfig;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    public LLMInvoker(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
        // 初始化 FreeMarker 配置
        this.freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        this.freeMarkerConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.freeMarkerConfig.setClassLoaderForTemplateLoading(
                Thread.currentThread().getContextClassLoader(), "templates");
        this.freeMarkerConfig.setAPIBuiltinEnabled(true);
    }

    public LLMInvoker() {
        this(null);
    }

    /**
     * 解析配置需求
     * 
     * @param userInput     用户输入
     * @param productSerials 产品系列列表（逗号分隔的字符串）
     * @return 配置需求
     */
    public ConfigReq parseConfigReq(String userInput, String productSerials) {
        if (chatModel == null) {
            log.warn("ChatModel not available, using simple parser");
            return parseConfigReqSimple(userInput, productSerials);
        }

        try {
            // 渲染 prompt
            String prompt = renderPrompt(userInput, productSerials);
            log.debug("Rendered prompt: {}", prompt);

            // 调用 LLM
            String response = callLLM(prompt);
            log.debug("LLM response: {}", response);

            // 解析 JSON 响应
            ConfigReq configReq = tryParseResponse(response);
            if (configReq == null) {
                log.warn("LLM response is not a valid ConfigReq JSON, fallback to simple parser. response={}", response);
                return parseConfigReqSimple(userInput, productSerials);
            }

            // 确保默认值和验证
            ensureDefaults(userInput, productSerials, configReq);
            return configReq;

        } catch (Exception e) {
            log.error("Failed to parse config req with LLM, fallback to simple parser", e);
            return parseConfigReqSimple(userInput, productSerials);
        }
    }

    /**
     * 渲染 prompt 模板
     */
    private String renderPrompt(String userInput, String productSerials) {
        try {
            Template template = freeMarkerConfig.getTemplate("bid-parser.ftl", StandardCharsets.UTF_8.name());
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("userInput", userInput != null ? userInput : "");
            
            // 解析产品系列列表
            List<String> serialsList = new ArrayList<>();
            if (StringUtils.hasText(productSerials)) {
                serialsList = Arrays.asList(productSerials.split(","));
            }
            dataModel.put("productSerials", serialsList);

            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return writer.toString();
        } catch (TemplateException | IOException e) {
            log.error("Failed to render prompt template", e);
            throw new IllegalStateException("Unable to render prompt template", e);
        }
    }

    /**
     * 调用 LLM
     */
    private String callLLM(String prompt) {
        long startTime = System.currentTimeMillis();
        
        if (log.isDebugEnabled()) {
            log.debug("=== LLM 调用开始 ===");
            log.debug("Prompt 长度: {} 字符", prompt != null ? prompt.length() : 0);
            // 打印 prompt 的前500个字符和后100个字符（如果超过600字符）
            if (prompt != null && prompt.length() > 600) {
                log.debug("Prompt 预览（前500字符）: {}", prompt.substring(0, Math.min(500, prompt.length())));
                log.debug("Prompt 预览（后100字符）: {}", prompt.substring(Math.max(0, prompt.length() - 100)));
            } else {
                log.debug("Prompt 内容: {}", prompt);
            }
        }
        
        try {
            Prompt aiPrompt = new Prompt(List.<Message>of(new UserMessage(prompt)));
            
            if (log.isDebugEnabled()) {
                log.debug("发送请求到 LLM...");
            }
            
            ChatResponse response = chatModel.call(aiPrompt);
            
            long callDuration = System.currentTimeMillis() - startTime;
            
            if (response == null) {
                if (log.isDebugEnabled()) {
                    log.debug("LLM 返回 null 响应，耗时: {} ms", callDuration);
                    log.debug("=== LLM 调用结束（无响应）===");
                }
                return "";
            }
            
            Generation generation = response.getResult();
            if (generation == null) {
                if (log.isDebugEnabled()) {
                    log.debug("LLM 响应中 Generation 为 null，耗时: {} ms", callDuration);
                    log.debug("=== LLM 调用结束（无 Generation）===");
                }
                return "";
            }
            
            // Spring AI 1.0.0-M4 使用 getOutput().getContent() 方法
            String content = generation.getOutput().getContent();
            
            if (log.isDebugEnabled()) {
                log.debug("LLM 调用成功，耗时: {} ms", callDuration);
                log.debug("响应长度: {} 字符", content != null ? content.length() : 0);
                
                // 打印响应的前500个字符和后100个字符（如果超过600字符）
                if (content != null && content.length() > 600) {
                    log.debug("响应预览（前500字符）: {}", content.substring(0, Math.min(500, content.length())));
                    log.debug("响应预览（后100字符）: {}", content.substring(Math.max(0, content.length() - 100)));
                } else {
                    log.debug("响应内容: {}", content);
                }
                
                // 记录元数据信息
                if (generation.getMetadata() != null) {
                    log.debug("响应元数据: {}", generation.getMetadata());
                }
                
                log.debug("=== LLM 调用结束 ===");
            }
            
            return content;
        } catch (Exception e) {
            long callDuration = System.currentTimeMillis() - startTime;
            
            if (log.isDebugEnabled()) {
                log.debug("LLM 调用失败，耗时: {} ms", callDuration);
                log.debug("异常类型: {}", e.getClass().getName());
                log.debug("异常消息: {}", e.getMessage());
                log.debug("=== LLM 调用结束（异常）===");
            }
            
            log.error("Failed to call LLM", e);
            throw new RuntimeException("Failed to call LLM", e);
        }
    }

    /**
     * 尝试解析 LLM 响应为 ConfigReq
     */
    private ConfigReq tryParseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        try {
            // 尝试提取 JSON（可能包含 markdown 代码块）
            String json = extractJson(response);
            return objectMapper.readValue(json, ConfigReq.class);
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse LLM response as JSON", e);
            return null;
        }
    }

    /**
     * 从响应中提取 JSON（可能包含在 markdown 代码块中）
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        // 如果包含 markdown 代码块，提取其中的内容
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("{");
            int end = trimmed.lastIndexOf("}");
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        // 直接查找 JSON 对象
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * 确保默认值和验证
     */
    private void ensureDefaults(String userInput, String productSerials, ConfigReq configReq) {
        // 验证产品系列是否在枚举中
        List<String> validSerials = new ArrayList<>();
        if (StringUtils.hasText(productSerials)) {
            validSerials = Arrays.asList(productSerials.split(","));
        }
        if (!validSerials.contains(configReq.getProductSerial())) {
            configReq.setProductSerial("");
        }

        // 确保配置策略不为空
        if (configReq.getConfigStrategy() == null) {
            configReq.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        }

        // 确保数量大于0
        if (configReq.getTotalQuantity() == null || configReq.getTotalQuantity() <= 0) {
            configReq.setTotalQuantity(1);
            configReq.setTotalQuantityMemo("没有明确指定说明，默认值配置1套");
        } else if (!StringUtils.hasText(configReq.getTotalQuantityMemo())) {
            // 检查是否真的在输入中找到了数量
            Optional<Integer> detectedQuantity = detectTotalQuantity(userInput);
            if (detectedQuantity.isEmpty()) {
                configReq.setTotalQuantityMemo("没有明确指定说明，默认值配置1套");
            }
        }

        // 确保规格项列表不为空
        if (configReq.getSpecReqItems() == null || configReq.getSpecReqItems().isEmpty()) {
            configReq.setSpecReqItems(extractSpecItems(userInput));
        }

        // 确保 memo 不为 null
        if (configReq.getTotalQuantityMemo() == null) {
            configReq.setTotalQuantityMemo("");
        }
    }

    /**
     * 检测总数量
     */
    private Optional<Integer> detectTotalQuantity(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("(\\d+)\\s*[台套]");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 提取规格项
     */
    private List<String> extractSpecItems(String userInput) {
        List<String> items = new ArrayList<>();
        if (userInput == null || userInput.trim().isEmpty()) {
            return items;
        }
        // 按分隔符拆分
        String[] parts = userInput.split("[\\n；;。、]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.matches(".*[≥>=<≤].*")) {
                items.add(trimmed);
            }
        }
        return items;
    }

    /**
     * 简单解析配置需求（当LLM不可用时使用）
     */
    private ConfigReq parseConfigReqSimple(String userInput, String productSerials) {
        ConfigReq req = new ConfigReq();
        req.setTotalQuantity(1);
        req.setTotalQuantityMemo("LLM不可用，使用简单解析");
        req.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        req.setSpecReqItems(new java.util.ArrayList<>());

        // 简单提取产品系列
        String[] serials = productSerials.split(",");
        for (String serial : serials) {
            if (userInput.contains(serial.trim())) {
                req.setProductSerial(serial.trim());
                break;
            }
        }

        // 简单提取数量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*[台套]");
        java.util.regex.Matcher matcher = pattern.matcher(userInput);
        if (matcher.find()) {
            req.setTotalQuantity(Integer.parseInt(matcher.group(1)));
        }

        // 简单提取规格项（按行分割）
        String[] lines = userInput.split("\n");
        for (String line : lines) {
            if (line.trim().matches(".*[≥>=<≤].*")) {
                req.getSpecReqItems().add(line.trim());
            }
        }

        return req;
    }
}

