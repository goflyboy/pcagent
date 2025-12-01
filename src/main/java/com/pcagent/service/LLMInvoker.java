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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
     * 调用 LLM 进行配置检查（公共方法，供其他服务调用）
     */
    public String callLLMForCheck(String prompt) {
        if (chatModel == null) {
            log.warn("ChatModel not available for config check");
            return null;
        }
        return callLLM(prompt);
    }

    /**
     * 调用 LLM
     */
    private String callLLM(String prompt) {
        long startTime = System.currentTimeMillis();
        
        // 打印 LLM 请求参数信息
        if (log.isDebugEnabled()) {
            logLLMRequestInfo();
        }
        
        try {
            Prompt aiPrompt = new Prompt(List.<Message>of(new UserMessage(prompt)));

            if (log.isDebugEnabled()) {
                log.debug("发送请求到 LLM...");
            }

            ChatResponse response = chatModel.call(aiPrompt);

            if (response == null) {
                long callDuration = System.currentTimeMillis() - startTime;
                if (log.isDebugEnabled()) {
                    log.debug("LLM 返回 null 响应，耗时: {} ms", callDuration);
                    log.debug("=== LLM 调用结束（无响应）===");
                }
                return "";
            }
            
            Generation generation = response.getResult();
            
            if (generation == null) {
                long callDuration = System.currentTimeMillis() - startTime;
                if (log.isDebugEnabled()) {
                    log.debug("LLM 响应中 Generation 为 null，耗时: {} ms", callDuration);
                    log.debug("=== LLM 调用结束（无 Generation）===");
                }
                return "";
            }

            // 为兼容 Spring AI 不同版本，这里通过反射获取文本内容：
            // - 优先尝试 output.getText()
            // - 然后尝试 output.getContent()
            // - 最后回退到 output.toString()
            Object output = generation.getOutput();
            String content = "";
            if (output != null) {
                try {
                    java.lang.reflect.Method getText = output.getClass().getMethod("getText");
                    Object v = getText.invoke(output);
                    content = v != null ? v.toString() : "";
                } catch (NoSuchMethodException ignore) {
                    try {
                        java.lang.reflect.Method getContent = output.getClass().getMethod("getContent");
                        Object v = getContent.invoke(output);
                        content = v != null ? v.toString() : "";
                    } catch (NoSuchMethodException ignore2) {
                        content = output.toString();
                    }
                }
            }
            
            long callDuration = System.currentTimeMillis() - startTime;
            
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
     * 打印 LLM 请求参数信息（URL、模型、temperature 等）
     */
    private void logLLMRequestInfo() {
        if (chatModel == null) {
            log.debug("LLM 请求参数: ChatModel 为 null，使用简单解析");
            return;
        }

        try {
            // 尝试获取 OpenAiChatModel 的配置信息
            if (chatModel instanceof OpenAiChatModel) {
                OpenAiChatModel openAiChatModel = (OpenAiChatModel) chatModel;
                
                // 通过反射获取 OpenAiApi
                Field apiField = OpenAiChatModel.class.getDeclaredField("openAiApi");
                apiField.setAccessible(true);
                OpenAiApi api = (OpenAiApi) apiField.get(openAiChatModel);
                
                // 获取 baseUrl
                String baseUrl = "未知";
                try {
                    Method getBaseUrlMethod = OpenAiApi.class.getMethod("getBaseUrl");
                    baseUrl = (String) getBaseUrlMethod.invoke(api);
                } catch (Exception e) {
                    // 如果方法不存在，尝试通过字段获取
                    try {
                        Field baseUrlField = OpenAiApi.class.getDeclaredField("baseUrl");
                        baseUrlField.setAccessible(true);
                        baseUrl = (String) baseUrlField.get(api);
                    } catch (Exception ex) {
                        log.debug("无法获取 baseUrl: {}", ex.getMessage());
                    }
                }
                
                // 通过反射获取 OpenAiChatOptions
                Field optionsField = OpenAiChatModel.class.getDeclaredField("defaultOptions");
                optionsField.setAccessible(true);
                OpenAiChatOptions options = (OpenAiChatOptions) optionsField.get(openAiChatModel);
                
                // 获取模型名称
                String model = "未知";
                if (options != null) {
                    try {
                        Method getModelMethod = OpenAiChatOptions.class.getMethod("getModel");
                        model = (String) getModelMethod.invoke(options);
                    } catch (Exception e) {
                        try {
                            Field modelField = OpenAiChatOptions.class.getDeclaredField("model");
                            modelField.setAccessible(true);
                            model = (String) modelField.get(options);
                        } catch (Exception ex) {
                            log.debug("无法获取 model: {}", ex.getMessage());
                        }
                    }
                }
                
                // 获取 temperature
                Double temperature = null;
                if (options != null) {
                    try {
                        Method getTemperatureMethod = OpenAiChatOptions.class.getMethod("getTemperature");
                        temperature = (Double) getTemperatureMethod.invoke(options);
                    } catch (Exception e) {
                        try {
                            Field temperatureField = OpenAiChatOptions.class.getDeclaredField("temperature");
                            temperatureField.setAccessible(true);
                            temperature = (Double) temperatureField.get(options);
                        } catch (Exception ex) {
                            log.debug("无法获取 temperature: {}", ex.getMessage());
                        }
                    }
                }
                
                // 打印请求参数信息
                log.debug("=== LLM 请求参数信息 ===");
                log.debug("Provider: {}", provider);
                log.debug("Base URL: {}", baseUrl);
                log.debug("Model: {}", model != null ? model : "未知");
                log.debug("Temperature: {}", temperature != null ? temperature : "未知");
                log.debug("ChatModel 类型: {}", chatModel.getClass().getName());
                log.debug("=========================");
            } else {
                log.debug("LLM 请求参数: ChatModel 类型为 {}, 无法提取详细配置信息", 
                        chatModel.getClass().getName());
            }
        } catch (Exception e) {
            log.debug("获取 LLM 请求参数信息失败: {}", e.getMessage());
            log.debug("ChatModel 类型: {}", chatModel != null ? chatModel.getClass().getName() : "null");
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

