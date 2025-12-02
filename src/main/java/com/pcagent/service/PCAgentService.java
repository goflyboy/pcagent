package com.pcagent.service;

import com.pcagent.controller.SessionVOConverter;
import com.pcagent.controller.vo.SessionVO;
import com.pcagent.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 产品配置Agent服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PCAgentService {
    private final Map<String, PCAgentService4Session> sessions = new HashMap<>();
    /**
     * 每个会话对应的 SSE 订阅者列表
     */
    private final Map<String, List<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();
    private final LLMInvoker llmInvoker;
    private final ProductSpecificationParserService specParserService;
    private final ProductSelectionService selectionService;
    private final ProductConfigService configService;
    private final SessionVOConverter sessionVOConverter;

    /**
     * 生成配置（异步执行）
     */
    public PCAgentService4Session doGeneratorConfig(String sessionId, String userInput) {
        PCAgentService4Session sessionService = new PCAgentService4Session(
                sessionId, llmInvoker, specParserService, selectionService, configService,
                this::onSessionUpdated);
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
            log.warn("No session found for sessionId: {}", sessionId);
            return null;
        }
        return sessionService.getCurrentSession();
    }

    /**
     * 注册会话的 SSE 订阅
     */
    public SseEmitter registerSessionEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时，由前端主动关闭

        sessionEmitters.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(e -> removeEmitter(sessionId, emitter));

        // 立即推送当前状态（如果已有）
        Session current = getLatestSession(sessionId);
        if (current != null) {
            SessionVO sessionVO = sessionVOConverter.convert(current);
            sendToEmitter(emitter, sessionVO);
        }

        return emitter;
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        List<SseEmitter> list = sessionEmitters.get(sessionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                sessionEmitters.remove(sessionId);
            }
        }
    }

    /**
     * 收到会话更新回调时，向所有 SSE 订阅者推送最新 SessionVO
     */
    private void onSessionUpdated(Session session) {
        if (session == null || session.getSessionId() == null) {
            return;
        }
        String sessionId = session.getSessionId();
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        SessionVO sessionVO = sessionVOConverter.convert(session);
        for (SseEmitter emitter : new ArrayList<>(emitters)) {
            sendToEmitter(emitter, sessionVO);
        }
    }

    private void sendToEmitter(SseEmitter emitter, SessionVO sessionVO) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("session-update")
                    .data(sessionVO, MediaType.APPLICATION_JSON);
            emitter.send(event);
        } catch (IOException e) {
            log.warn("Failed to send SSE event: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}

