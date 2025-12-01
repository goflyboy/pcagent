package com.pcagent.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义REST API ChatModel适配器
 * 用于调用非标准OpenAPI接口的第三方大模型API
 * 
 * 配置说明：
 * - CUSTOM_LLM_API_URL: 第三方API的完整URL（必需）
 * - CUSTOM_LLM_API_KEY: API密钥（可选，根据实际需求）
 * - CUSTOM_LLM_MODEL: 模型名称（可选）
 * - CUSTOM_LLM_REQUEST_BODY_TEMPLATE: 请求体模板（可选，JSON格式）
 * 
 * 请求体模板支持以下占位符：
 * - {messages}: 将被替换为消息列表
 * - {prompt}: 将被替换为合并后的提示文本
 * - {model}: 将被替换为模型名称
 * - {temperature}: 将被替换为温度参数（默认0.7）
 */
@Slf4j
public class CustomRestChatModel implements ChatModel {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final String requestBodyTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     * 
     * @param apiUrl API的完整URL
     * @param apiKey API密钥（可为null）
     * @param model 模型名称（可为null）
     * @param temperature 温度参数（可为null，默认0.7）
     * @param requestBodyTemplate 请求体模板（可为null，使用默认模板）
     */
    public CustomRestChatModel(String apiUrl, String apiKey, String model, 
                               Double temperature, String requestBodyTemplate) {
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new IllegalArgumentException("API URL cannot be null or empty");
        }
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature != null ? temperature : 0.7;
        this.requestBodyTemplate = requestBodyTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 从环境变量创建CustomRestChatModel实例
     * 
     * @return CustomRestChatModel实例，如果环境变量不可用则返回null
     */
    public static CustomRestChatModel createIfAvailable() {
        String apiUrl = getEnvVar("CUSTOM_LLM_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            return null;
        }

        String apiKey = getEnvVar("CUSTOM_LLM_API_KEY");
        String model = getEnvVar("CUSTOM_LLM_MODEL");
        String temperatureStr = getEnvVar("CUSTOM_LLM_TEMPERATURE");
        Double temperature = temperatureStr != null ? Double.parseDouble(temperatureStr) : null;
        String requestBodyTemplate = getEnvVar("CUSTOM_LLM_REQUEST_BODY_TEMPLATE");

        try {
            return new CustomRestChatModel(apiUrl, apiKey, model, temperature, requestBodyTemplate);
        } catch (Exception e) {
            log.error("Failed to create CustomRestChatModel: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ChatOptions getDefaultOptions() { 
        // 返回null，因为自定义REST API不需要标准的ChatOptions
        // 配置通过构造函数参数和环境变量管理
        return null;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            // 构建请求
            HttpHeaders headers = buildHeaders();
            String requestBody = buildRequestBody(prompt);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling custom LLM API: {}", apiUrl);
            log.debug("Request body: {}", requestBody);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            log.debug("API response: {}", responseBody);

            // 解析响应
            return parseResponse(responseBody);

        } catch (RestClientException e) {
            log.error("Failed to call custom LLM API", e);
            throw new RuntimeException("Failed to call custom LLM API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while calling custom LLM API", e);
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * 构建HTTP请求头
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            // 根据实际API需求设置认证头
            // 常见格式：Authorization: Bearer {apiKey} 或 X-API-Key: {apiKey}
            // 这里默认使用Bearer格式，如果需要其他格式，可以通过环境变量配置
            String authHeader = getEnvVar("CUSTOM_LLM_AUTH_HEADER", "Bearer");
            if ("Bearer".equals(authHeader)) {
                headers.set("Authorization", "Bearer " + apiKey);
            } else if ("X-API-Key".equals(authHeader)) {
                headers.set("X-API-Key", apiKey);
            } else {
                // 自定义格式：CUSTOM_LLM_AUTH_HEADER=HeaderName:HeaderValue
                String[] parts = authHeader.split(":", 2);
                if (parts.length == 2) {
                    headers.set(parts[0], parts[1].replace("{apiKey}", apiKey));
                } else {
                    headers.set("Authorization", "Bearer " + apiKey);
                }
            }
        }

        return headers;
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(Prompt prompt) {
        try {
            // 合并所有消息为单个提示文本
            String promptText = mergeMessages(prompt.getInstructions());

            // 如果提供了自定义模板，使用模板
            if (requestBodyTemplate != null && !requestBodyTemplate.isEmpty()) {
                return buildRequestBodyFromTemplate(promptText);
            }

            // 否则使用默认模板
            return buildDefaultRequestBody(promptText);

        } catch (Exception e) {
            log.error("Failed to build request body", e);
            throw new RuntimeException("Failed to build request body: " + e.getMessage(), e);
        }
    }

    /**
     * 从模板构建请求体
     */
    private String buildRequestBodyFromTemplate(String promptText) {
        String body = requestBodyTemplate;
        
        // 替换占位符
        body = body.replace("{prompt}", escapeJson(promptText));
        if (model != null) {
            body = body.replace("{model}", escapeJson(model));
        }
        body = body.replace("{temperature}", String.valueOf(temperature));
        
        return body;
    }

    /**
     * 构建默认请求体
     */
    private String buildDefaultRequestBody(String promptText) {
        Map<String, Object> requestMap = new HashMap<>();
        
        if (model != null) {
            requestMap.put("model", model);
        }
        
        requestMap.put("prompt", promptText);
        requestMap.put("temperature", temperature);
        
        try {
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request body", e);
        }
    }

    /**
     * 合并消息列表为单个文本
     */
    private String mergeMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            if (message.getContent() != null) {
                sb.append(message.getContent()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 解析API响应为ChatResponse
     */
    private ChatResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // 尝试从常见字段中提取内容
            String content = extractContent(jsonNode);
            
            if (content == null) {
                log.warn("Could not extract content from response, using raw response");
                content = responseBody;
            }

            // 创建AssistantMessage
            AssistantMessage assistantMessage = new AssistantMessage(content);
            
            // 创建Generation
            Generation generation = new Generation(assistantMessage);
            
            // 创建ChatResponse
            return new ChatResponse(List.of(generation));

        } catch (Exception e) {
            log.error("Failed to parse API response", e);
            // 如果解析失败，尝试将整个响应作为内容
            AssistantMessage assistantMessage = new AssistantMessage(responseBody);
            Generation generation = new Generation(assistantMessage);
            return new ChatResponse(List.of(generation));
        }
    }

    /**
     * 从JSON响应中提取内容
     * 支持常见的响应格式：
     * - response.text
     * - response.content
     * - result.text
     * - data.content
     * - choices[0].message.content (OpenAI格式)
     * - choices[0].text
     */
    private String extractContent(JsonNode jsonNode) {
        // 尝试多种可能的字段路径
        String[] paths = {
            "response/text",
            "response/content",
            "result/text",
            "result/content",
            "data/content",
            "data/text",
            "content",
            "text",
            "message/content",
            "message/text"
        };

        for (String path : paths) {
            String content = getNestedValue(jsonNode, path);
            if (content != null && !content.isEmpty()) {
                return content;
            }
        }

        // 尝试OpenAI格式：choices[0].message.content
        if (jsonNode.has("choices") && jsonNode.get("choices").isArray() 
            && jsonNode.get("choices").size() > 0) {
            JsonNode choice = jsonNode.get("choices").get(0);
            if (choice.has("message") && choice.get("message").has("content")) {
                return choice.get("message").get("content").asText();
            }
            if (choice.has("text")) {
                return choice.get("text").asText();
            }
        }

        return null;
    }

    /**
     * 获取嵌套JSON值
     */
    private String getNestedValue(JsonNode node, String path) {
        String[] parts = path.split("/");
        JsonNode current = node;
        
        for (String part : parts) {
            if (current == null || !current.has(part)) {
                return null;
            }
            current = current.get(part);
        }
        
        return current != null && current.isTextual() ? current.asText() : null;
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 获取环境变量
     */
    private static String getEnvVar(String key) {
        return getEnvVar(key, null);
    }

    /**
     * 获取环境变量（带默认值）
     */
    private static String getEnvVar(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}

