package com.pcagent.service;

import com.pcagent.exception.InvalidInputException;
import com.pcagent.model.*;
import com.pcagent.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.pcagent.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 生成配置
     */
    public PCAgentService4Session doGeneratorConfig(String sessionId, String userInput) {
        PCAgentService4Session sessionService = new PCAgentService4Session(
                sessionId, llmInvoker, specParserService, selectionService, configService);
        sessionService.doGeneratorConfig(userInput);
        sessions.put(sessionId, sessionService);
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

