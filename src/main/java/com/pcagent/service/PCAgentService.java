package com.pcagent.service;

import com.pcagent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 产品配置Agent服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PCAgentService {
    private final Map<String, PCAgentService4Session> sessions = new HashMap<>();
    private final LLMInvoker llmInvoker;
    private final ProductSpecificationParserService specParserService;
    private final ProductSelectionService selectionService;
    private final ProductConfigService configService;

    /**
     * 生成配置（异步执行）
     */
    public PCAgentService4Session doGeneratorConfig(String sessionId, String userInput) {
        PCAgentService4Session sessionService = new PCAgentService4Session(
                sessionId, llmInvoker, specParserService, selectionService, configService);
        sessions.put(sessionId, sessionService);
        
        // 异步执行配置生成
        CompletableFuture.runAsync(() -> {
            try {
                sessionService.doGeneratorConfig(userInput);
                log.debug("配置生成完成，sessionId: {}", sessionId);
            } catch (Exception e) {
                log.error("异步配置生成失败，sessionId: {}", sessionId, e);
            }
        });
        
        return sessionService;
    }

    /**
     * 获取最新会话
     */
    public Session getLatestSession(String sessionId) {
        PCAgentService4Session sessionService = sessions.get(sessionId);
        if (sessionService == null) {
            return null;
        }
        return sessionService.getCurrentSession();
    }
}

