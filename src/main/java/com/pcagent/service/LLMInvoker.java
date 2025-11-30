package com.pcagent.service;

import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM调用服务
 * 支持deepseek和Qinwen两个模型
 */
@Slf4j
@Service
public class LLMInvoker {
    private final ChatClient chatClient;
    
    @Value("${spring.ai.provider:openai}")
    private String provider;

    @Autowired(required = false)
    public LLMInvoker(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 解析配置需求
     * 
     * @param userInput     用户输入
     * @param productSerials 产品系列列表
     * @return 配置需求
     */
    public ConfigReq parseConfigReq(String userInput, String productSerials) {
        String promptTemplate = """
            假设你是一名标书解析专家，请根据用户的输入{userInput}来解析标书，要求如下：
            
            1、解析要求：
            --产品系列仅是例举的清单，清单有{productSerials}，否则输出为空""
            --总套数也是有明确要求，否则给出说明totalQuantityMemo（例如：没有明确指定说明，默认值配置1套）
            --规格项拆分原则：按分割符拆分
            --配置策略，根据用户的诉求推理策略，默认PRICE_MIN_PRIORITY
            
            2、输出格式为JSON：
            {
              "productSerial": "产品系列",
              "totalQuantity": 总套数,
              "specReqItems": ["规格需求项1", "规格需求项2"],
              "configStrategy": "PRICE_MIN_PRIORITY" 或 "TECH_MAX_PRIORITY",
              "totalQuantityMemo": "总套数说明"
            }
            
            用户输入：{userInput}
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("userInput", userInput);
        variables.put("productSerials", productSerials);

        PromptTemplate template = new PromptTemplate(promptTemplate);
        Prompt prompt = template.create(variables);

        try {
            if (chatClient == null) {
                log.warn("ChatClient not available, using simple parsing");
                return parseConfigReqSimple(userInput, productSerials);
            }
            
            ChatResponse response = chatClient.call(prompt);
            String content = response.getResult().getOutput().getContent();
            log.info("LLM response: {}", content);
            
            // 解析JSON响应
            return parseJsonResponse(content);
        } catch (Exception e) {
            log.error("Failed to parse config req from LLM", e);
            // 返回简单解析结果
            return parseConfigReqSimple(userInput, productSerials);
        }
    }

    /**
     * 解析JSON响应
     */
    private ConfigReq parseJsonResponse(String response) {
        // 提取JSON部分
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}") + 1;
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String jsonStr = response.substring(jsonStart, jsonEnd);
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(jsonStr, ConfigReq.class);
            } catch (Exception e) {
                log.error("Failed to parse JSON response", e);
            }
        }
        
        // 如果解析失败，返回默认值
        ConfigReq defaultReq = new ConfigReq();
        defaultReq.setTotalQuantity(1);
        defaultReq.setTotalQuantityMemo("JSON解析失败，使用默认值");
        return defaultReq;
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

