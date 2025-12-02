package com.pcagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcagent.exception.InvalidInputException;
import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import com.pcagent.util.StringHelper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 配置需求识别服务
 * 负责解析和校验配置需求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigReqRecognitionService {
    private final LLMInvoker llmInvoker;
    private final ObjectMapper objectMapper;
    private final Configuration freeMarkerConfig;

    public ConfigReqRecognitionService(LLMInvoker llmInvoker) {
        this.llmInvoker = llmInvoker;
        this.objectMapper = new ObjectMapper();
        // 初始化 FreeMarker 配置
        this.freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        this.freeMarkerConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
        this.freeMarkerConfig.setClassLoaderForTemplateLoading(
                Thread.currentThread().getContextClassLoader(), "templates");
        this.freeMarkerConfig.setAPIBuiltinEnabled(true);
    }

    /**
     * 解析配置需求
     * 
     * @param input 用户输入
     * @return 配置需求
     */
    public ConfigReq parseConfigReq(String input) {
        String productSerials = "ONU,电脑,服务器,路由器";
        
        if (llmInvoker == null || !llmInvoker.isAvailable()) {
            log.warn("ChatModel not available, using simple parser");
            ConfigReq req = parseConfigReqSimple(input, productSerials);
            // 规范化 specReqItems
            normalizeSpecReqItems(req);
            return req;
        }

        try {
            // 渲染 prompt
            String prompt = renderPrompt(input, productSerials);
            log.debug("Rendered prompt: {}", prompt);

            // 调用 LLM
            String response = llmInvoker.callLLM(prompt);
            log.debug("LLM response: {}", response);

            // 解析 JSON 响应
            ConfigReq configReq = tryParseResponse(response);
            if (configReq == null) {
                log.warn("LLM response is not a valid ConfigReq JSON, fallback to simple parser. response={}", response);
                ConfigReq req = parseConfigReqSimple(input, productSerials);
                normalizeSpecReqItems(req);
                return req;
            }

            // 确保默认值和验证
            ensureDefaults(input, productSerials, configReq);
            
            // 规范化 specReqItems：去掉空格，替换特殊字符
            normalizeSpecReqItems(configReq);
            
            return configReq;

        } catch (Exception e) {
            log.error("Failed to parse config req with LLM, fallback to simple parser", e);
            ConfigReq req = parseConfigReqSimple(input, productSerials);
            normalizeSpecReqItems(req);
            return req;
        }
    }

    /**
     * 规范化 specReqItems
     */
    private void normalizeSpecReqItems(ConfigReq req) {
        if (req != null && req.getSpecReqItems() != null && !req.getSpecReqItems().isEmpty()) {
            List<String> normalizedItems = StringHelper.normalizeSpecReqItems(req.getSpecReqItems());
            req.setSpecReqItems(normalizedItems);
            log.debug("Normalized specReqItems: {} items", normalizedItems.size());
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
        
        // 确保国家/地区字段：如果 LLM 没有返回，尝试从输入中提取
        if (!StringUtils.hasText(configReq.getCountry())) {
            String country = extractCountry(userInput);
            configReq.setCountry(country);
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
        req.setSpecReqItems(new ArrayList<>());

        // 简单提取产品系列
        String[] serials = productSerials.split(",");
        for (String serial : serials) {
            if (userInput.contains(serial.trim())) {
                req.setProductSerial(serial.trim());
                break;
            }
        }

        // 简单提取数量
        Pattern pattern = Pattern.compile("(\\d+)\\s*[台套]");
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
        
        // 简单提取国家/地区
        String country = extractCountry(userInput);
        req.setCountry(country);

        return req;
    }
    
    /**
     * 从用户输入中提取国家/地区信息
     * 
     * @param userInput 用户输入
     * @return 国家/地区，如果未找到则返回空字符串
     */
    private String extractCountry(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return "";
        }
        
        // 常见国家/地区关键词
        String[] countries = {
            "中国", "美国", "日本", "韩国", "德国", "法国", "英国", "意大利", "西班牙",
            "俄罗斯", "印度", "巴西", "澳大利亚", "加拿大", "墨西哥", "阿根廷",
            "欧洲", "亚洲", "北美", "南美", "非洲", "大洋洲"
        };
        
        for (String country : countries) {
            if (userInput.contains(country)) {
                return country;
            }
        }
        
        return "";
    }

    /**
     * 校验配置需求
     * 
     * @param req 配置需求
     * @throws InvalidInputException 如果配置需求无效
     */
    public void validConfigReq(ConfigReq req) {
        if (req == null) {
            throw new InvalidInputException("配置需求不能为空");
        }
        if (req.getProductSerial() == null || req.getProductSerial().trim().isEmpty()) {
            throw new InvalidInputException("产品系列不能为空");
        }
        if (req.getTotalQuantity() == null || req.getTotalQuantity() <= 0) {
            throw new InvalidInputException("总套数必须大于0");
        }
    }
}

