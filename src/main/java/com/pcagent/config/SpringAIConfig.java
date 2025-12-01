package com.pcagent.config;

import com.pcagent.util.ChatModelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI配置类
 * 参考 PCAgentService4SessionSysTest 的初始化逻辑
 * 
 * 初始化逻辑：
 * 1. 尝试创建真实的 ChatModel（通过 ChatModelBuilder.createIfAvailable()）
 * 2. 如果 ChatModel 可用，LLMInvoker 会自动注入它
 * 3. 如果 ChatModel 不可用，LLMInvoker 会使用无参构造函数（使用简单解析）
 * 4. ProductConfigService 会自动注入 LLMInvoker（无论它是否有 ChatModel）
 * 
 * 支持的ChatModel类型（按优先级）：
 * 
 * 1. 自定义REST API（CustomRestChatModel）：
 *    - CUSTOM_LLM_API_URL: 第三方API的完整URL（必需）
 *    - CUSTOM_LLM_API_KEY: API密钥（可选）
 *    - CUSTOM_LLM_MODEL: 模型名称（可选）
 *    - CUSTOM_LLM_TEMPERATURE: 温度参数（可选，默认0.7）
 *    - CUSTOM_LLM_REQUEST_BODY_TEMPLATE: 请求体模板（可选，JSON格式，支持占位符：{prompt}, {model}, {temperature}）
 *    - CUSTOM_LLM_AUTH_HEADER: 认证头格式（可选，默认Bearer，支持：Bearer、X-API-Key或自定义格式如"HeaderName:HeaderValue"）
 * 
 * 2. DeepSeek：
 *    - DEEPSEEK_API_KEY: DeepSeek API key
 *    - DEEPSEEK_BASE_URL: DeepSeek API base URL (可选，默认: https://api.deepseek.com)
 * 
 * 3. 通义千问（Qinwen）：
 *    - QINWEN_API_KEY: 通义千问 API key
 *    - QINWEN_BASE_URL: 通义千问 API base URL (可选，默认: https://dashscope.aliyuncs.com/compatible-mode/v1)
 */
@Slf4j
@Configuration
public class SpringAIConfig {

    /**
     * 创建 ChatModel Bean（如果环境变量可用）
     * 参考 PCAgentService4SessionSysTest.setUp() 中的初始化逻辑
     * 
     * 如果环境变量不可用，返回 null，LLMInvoker 会使用无参构造函数
     * 
     * @return ChatModel 实例，如果环境变量不可用则返回 null
     */
    @Bean
    public ChatModel chatModel() {
        ChatModel chatModel = ChatModelBuilder.createIfAvailable();
        if (chatModel != null) {
            log.info("ChatModel created successfully");
        } else {
            log.info("ChatModel not available, LLMInvoker will use simple parser");
        }
        return chatModel;
    }
}

