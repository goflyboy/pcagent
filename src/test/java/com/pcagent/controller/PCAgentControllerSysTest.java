package com.pcagent.controller;

import com.pcagent.model.ProductConfig;
import com.pcagent.model.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PCAgentController 系统测试类
 * 需要启动 PcAgentApplication 才能运行
 * 使用 Spring Boot 测试框架测试 REST 接口
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PCAgentControllerSysTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    /**
     * 测试 createSession - 创建会话（使用真实 LLM）
     * 注意：此测试需要设置环境变量才能运行
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldCreateSession_WithRealLlm() throws Exception {
        // 准备测试数据
        Map<String, Object> request = new HashMap<>();
        request.put("user_input", """
                我有一名高端客户，需要建立数据中心，要求如下：
                数据中心服务器 512台
                1. CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核。
                2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。
                 """);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // 执行测试
        ResponseEntity<Session> response = restTemplate.exchange(
                getBaseUrl() + "/sessions",
                HttpMethod.POST,
                entity,
                Session.class
        );

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Session session = response.getBody();
        assertNotNull(session, "Session不应为null");
        assertNotNull(session.getSessionId(), "Session ID不应为null");
        assertNotNull(session.getProgress(), "Progress不应为null");
        assertEquals(3, session.getProgress().getCurrent(), "进度应该完成");
        assertEquals("配置完成", session.getProgress().getMessage());

        // 验证最终结果是 ProductConfig
        assertNotNull(session.getData(), "Session data不应为null");
        assertTrue(session.getData() instanceof ProductConfig, 
                "最终数据应该是 ProductConfig");
        
        ProductConfig productConfig = (ProductConfig) session.getData();
        assertNotNull(productConfig.getProductCode(), "产品代码不应为null");
        assertNotNull(productConfig.getParas(), "参数列表不应为null");
        assertTrue(productConfig.getParas().size() > 0, "应该至少有一个参数");
    } 
}

