package com.pcagent.util;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * ChatModel 构建器
 * 用于根据环境变量创建 ChatModel 实例
 * 支持 DeepSeek 和通义千问（Qinwen）
 * 
 * 注意：需要设置环境变量才能创建 ChatModel：
 * - DEEPSEEK_API_KEY: DeepSeek API key
 * - DEEPSEEK_BASE_URL: DeepSeek API base URL (可选，默认: https://api.deepseek.com)
 * 或
 * - QINWEN_API_KEY: 通义千问 API key
 * - QINWEN_BASE_URL: 通义千问 API base URL (可选，默认: https://dashscope.aliyuncs.com/compatible-mode/v1)
 */
public class ChatModelBuilder {

    private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final String DEFAULT_QINWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String DEFAULT_DEEPSEEK_MODEL = "deepseek-chat";
    private static final String DEFAULT_QINWEN_MODEL = "qwen-turbo";

    /**
     * 创建 ChatModel（如果环境变量可用）
     * 优先尝试 DeepSeek，如果不可用则尝试通义千问
     * 
     * @return ChatModel 实例，如果环境变量不可用则返回 null
     */
    public static ChatModel createIfAvailable() {
        // 优先尝试 DeepSeek
        String deepseekApiKey = getEnvVar("DEEPSEEK_API_KEY");
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            String baseUrl = getEnvVar("DEEPSEEK_BASE_URL", DEFAULT_DEEPSEEK_BASE_URL);
            return createChatModel(deepseekApiKey, baseUrl, DEFAULT_DEEPSEEK_MODEL);
        }

        // 尝试通义千问
        String qinwenApiKey = getEnvVar("QINWEN_API_KEY");
        if (qinwenApiKey != null && !qinwenApiKey.isEmpty()) {
            String baseUrl = getEnvVar("QINWEN_BASE_URL", DEFAULT_QINWEN_BASE_URL);
            return createChatModel(qinwenApiKey, baseUrl, DEFAULT_QINWEN_MODEL);
        }

        // 如果都没有，返回 null
        return null;
    }

    /**
     * 创建 ChatModel
     * 
     * @param apiKey API密钥
     * @param baseUrl API基础URL
     * @param modelName 模型名称
     * @return ChatModel 实例，如果创建失败则返回 null
     */
    private static ChatModel createChatModel(String apiKey, String baseUrl, String modelName) {
        try {
            OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(modelName)
                    .withTemperature(0.7)
                    .build();
            return new OpenAiChatModel(openAiApi, options);
        } catch (Exception e) {
            System.err.println("Failed to create ChatModel: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取环境变量
     * 
     * @param key 环境变量键
     * @return 环境变量值，如果不存在则返回 null
     */
    private static String getEnvVar(String key) {
        return getEnvVar(key, null);
    }

    /**
     * 获取环境变量（带默认值）
     * 
     * @param key 环境变量键
     * @param defaultValue 默认值
     * @return 环境变量值，如果不存在则返回默认值
     */
    private static String getEnvVar(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}

