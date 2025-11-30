package com.pcagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI配置类
 * 注意：由于Spring AI 1.0.0-M4的API可能在不同版本间有变化，
 * 如果自动配置失败，LLMInvoker会使用简单解析作为fallback
 * 
 * 如果需要使用LLM，请根据实际Spring AI版本手动配置ChatClient Bean
 */
@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    // 如果需要手动配置ChatClient，可以取消下面的注释并根据实际API调整
    /*
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient chatClient() {
        // 根据Spring AI实际版本调整此处的实现
        // 示例（需要根据实际API调整）:
        // return new OpenAiChatClient(apiKey, ...);
        return null;
    }
    */
}

