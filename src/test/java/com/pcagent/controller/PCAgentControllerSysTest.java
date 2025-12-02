package com.pcagent.controller;

import com.pcagent.controller.vo.ParameterConfigVO;
import com.pcagent.controller.vo.SessionVO;
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
                2. 内存：配置≥256GB DDR4 ECC Registered内存。
                 """);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // 执行测试 - 创建会话
        ResponseEntity<SessionVO> response = restTemplate.exchange(
                getBaseUrl() + "/sessions",
                HttpMethod.POST,
                entity,
                SessionVO.class
        );

        // 验证初始响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SessionVO sessionVO = response.getBody();
        assertNotNull(sessionVO, "SessionVO不应为null");
        assertNotNull(sessionVO.getSessionId(), "Session ID不应为null");
        assertNotNull(sessionVO.getProgress(), "Progress不应为null");
        
        String sessionId = sessionVO.getSessionId();
        
        // 等待异步任务完成（最多等待60秒）
        SessionVO finalSessionVO = null;
        int maxWaitSeconds = 60;
        int waitIntervalMs = 500;
        int maxAttempts = maxWaitSeconds * 1000 / waitIntervalMs;
        boolean taskCompleted = false;
        
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(waitIntervalMs);
            
            // 轮询获取最新状态
            ResponseEntity<SessionVO> statusResponse = restTemplate.exchange(
                    getBaseUrl() + "/sessions/" + sessionId,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    SessionVO.class
            );
            
            if (statusResponse.getStatusCode() == HttpStatus.OK) {
                finalSessionVO = statusResponse.getBody();
                if (finalSessionVO != null && 
                    finalSessionVO.getProgress() != null &&
                    finalSessionVO.getProgress().getCurrent() >= finalSessionVO.getProgress().getTotal()) {
                    // 任务完成
                    taskCompleted = true;
                    break;
                }
            }
        }
        
        // 如果任务未完成，使用最后一次获取的状态
        if (!taskCompleted && finalSessionVO == null) {
            // 最后一次尝试获取状态
            ResponseEntity<SessionVO> lastResponse = restTemplate.exchange(
                    getBaseUrl() + "/sessions/" + sessionId,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    SessionVO.class
            );
            if (lastResponse.getStatusCode() == HttpStatus.OK) {
                finalSessionVO = lastResponse.getBody();
            }
        }
        
        // 验证最终结果
        assertNotNull(finalSessionVO, "最终SessionVO不应为null");
        assertNotNull(finalSessionVO.getProgress(), "Progress不应为null");
        assertEquals(3, finalSessionVO.getProgress().getCurrent(), "进度应该完成");
        assertEquals(3, finalSessionVO.getProgress().getTotal(), "总进度应该是3");
        
        // 验证进度消息（应该是"配置完成"或包含"完成"）
        String progressMessage = finalSessionVO.getProgress().getMessage();
        assertNotNull(progressMessage, "进度消息不应为null");
        assertTrue(progressMessage.contains("完成") || progressMessage.contains("失败"), 
                "进度消息应该包含'完成'或'失败'");
        
        // 如果配置成功，验证最终结果是 ParameterConfigVO
        if (progressMessage.contains("完成")) {
            assertNotNull(finalSessionVO.getDisplayData(), "DisplayData不应为null");
            assertTrue(finalSessionVO.getDisplayData() instanceof ParameterConfigVO, 
                    "最终数据应该是 ParameterConfigVO");
            
            ParameterConfigVO parameterConfigVO = (ParameterConfigVO) finalSessionVO.getDisplayData();
            assertNotNull(parameterConfigVO.getProductCode(), "产品代码不应为null");
            assertNotNull(parameterConfigVO.getItems(), "参数列表不应为null");
            assertTrue(parameterConfigVO.getItems().size() > 0, "应该至少有一个参数");
        }
    } 
}

