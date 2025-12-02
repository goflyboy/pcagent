package com.pcagent.service;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;

/**
 * LLM调用服务
 * 纯粹的LLM调用服务，不包含业务逻辑
 * 支持deepseek和Qinwen两个模型
 */
@Slf4j
@Service
public class LLMInvoker {
    @Value("${spring.ai.provider:openai}")
    private String provider;

    private final ChatModel chatModel;

    @Autowired(required = false)
    public LLMInvoker(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public LLMInvoker() {
        this(null);
    }

    /**
     * 检查LLM是否可用
     */
    public boolean isAvailable() {
        return chatModel != null;
    }


    /**
     * 调用 LLM（公共方法，供其他服务调用）
     * 
     * @param prompt 提示词
     * @return LLM 响应内容
     */
    public String callLLM(String prompt) {
        if (chatModel == null) {
            log.warn("ChatModel not available");
            return null;
        }
        
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

}

