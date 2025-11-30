package com.pcagent.controller;

import com.pcagent.model.Session;
import com.pcagent.service.PCAgentService;
import com.pcagent.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 产品配置Agent控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PCAgentController {
    private final PCAgentService agentService;

    /**
     * 创建会话接口
     */
    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody Map<String, Object> request) {
        String userInput = (String) request.get("user_input");
        if (userInput == null || userInput.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String sessionId = SessionUtils.nextSessionId();
        agentService.doGeneratorConfig(sessionId, userInput);
        Session session = agentService.getLatestSession(sessionId);
        
        return ResponseEntity.ok(session);
    }

    /**
     * 继续会话接口
     */
    @PostMapping("/sessions/{session_id}/continue")
    public ResponseEntity<Session> continueSession(
            @PathVariable("session_id") String sessionId,
            @RequestBody Map<String, Object> request) {
        // TODO: 实现继续会话逻辑
        Session session = agentService.getLatestSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * 获取会话状态接口
     */
    @GetMapping("/sessions/{session_id}")
    public ResponseEntity<Session> getSession(@PathVariable("session_id") String sessionId) {
        Session session = agentService.getLatestSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * 确认操作接口
     */
    @PostMapping("/sessions/{session_id}/confirm")
    public ResponseEntity<Session> confirmSession(
            @PathVariable("session_id") String sessionId,
            @RequestBody Map<String, Object> request) {
        // TODO: 实现确认操作逻辑
        Session session = agentService.getLatestSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * 销毁操作接口
     */
    @PostMapping("/sessions/{session_id}/terminate")
    public ResponseEntity<Map<String, String>> terminateSession(
            @PathVariable("session_id") String sessionId) {
        // TODO: 实现销毁操作逻辑
        Map<String, String> response = new HashMap<>();
        response.put("status", "terminated");
        response.put("session_id", sessionId);
        return ResponseEntity.ok(response);
    }
}

