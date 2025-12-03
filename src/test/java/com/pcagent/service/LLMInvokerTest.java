package com.pcagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import com.pcagent.testcore.EnableLLMCache;
import com.pcagent.testcore.LLMCacheTestExtension;
import com.pcagent.util.ChatModelBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LLMInvoker 测试类
 * 简单的单元测试，使用 Mockito mock ChatModel
 */
@ExtendWith(LLMCacheTestExtension.class)
@Slf4j
class LLMInvokerTest { 
    @BeforeEach
    void setUp() { 
    }
 
    /**
     * 测试 callLLM 方法 - ChatModel 为 null 的情况
     */
    @Test
    @EnableLLMCache(true)
    //@EnableLLMCache(false)
    void testCallLLM() {
              // 尝试创建真实的 ChatModel
        ChatModel chatModel = ChatModelBuilder.createIfAvailable();
        LLMInvoker invokerWithoutModel = new LLMInvoker(chatModel);        
        String result = invokerWithoutModel.callLLM("你好，请介绍一下自己");
        log.info("result: {}", result);
        assertNotNull(result);
    }
 
}

