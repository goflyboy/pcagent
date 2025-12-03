package com.pcagent.service;

import com.pcagent.model.*;
import com.pcagent.service.impl.ProductOntoService4Local;
import com.pcagent.testcore.EnableLLMCache;
import com.pcagent.testcore.LLMCacheTestExtension;
import com.pcagent.util.ChatModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PCAgentService4Session 系统测试类
 * 使用真实的 ChatModel（不 mock）来测试完整流程
 * 参考 PCAgentService4SessionTest.testDoGeneratorConfig_CompleteFlow 编写
 * 
 * 注意：需要设置环境变量才能运行：
 * - DEEPSEEK_API_KEY: DeepSeek API key
 * - DEEPSEEK_BASE_URL: DeepSeek API base URL (可选，默认: https://api.deepseek.com)
 * 或
 * - QINWEN_API_KEY: 通义千问 API key
 * - QINWEN_BASE_URL: 通义千问 API base URL (可选，默认: https://dashscope.aliyuncs.com/compatible-mode/v1)
 * 
 * 使用 @EnableLLMCache 注解可以控制缓存行为：
 * - 类级别：所有测试方法使用相同的缓存设置
 * - 方法级别：可以覆盖类级别的设置
 */
@ExtendWith(LLMCacheTestExtension.class)
class PCAgentService4SessionSysTest {

    private LLMInvoker llmInvoker;
    private ProductSpecificationParserService specParserService;
    private ProductSelectionService selectionService;
    private ProductConfigService configService;
    private ProductOntoService4Local productOntoService;
    private PCAgentService4Session sessionService;

    @BeforeEach
    void setUp() { 
        // 初始化 ProductOntoService
        productOntoService = new ProductOntoService4Local();
        productOntoService.init();

        // 尝试创建真实的 ChatModel
        ChatModel chatModel = ChatModelBuilder.createIfAvailable();
        if (chatModel != null) {
            llmInvoker = new LLMInvoker(chatModel);
        } else {
            // 如果没有可用的 ChatModel，使用无参构造函数（会使用简单解析）
            llmInvoker = new LLMInvoker();
        }

        // 使用真实的 ProductSpecificationParserService
        specParserService = new ProductSpecificationParserService(productOntoService);

        // 使用真实的 ProductSelectionService
        selectionService = new ProductSelectionService(productOntoService);

        // 使用真实的 ProductConfigService（需要 LLMInvoker）
        if (chatModel != null) {
            configService = new ProductConfigService(productOntoService, llmInvoker);
        } else {
            configService = new ProductConfigService(productOntoService);
        }

        // 创建会话服务
        String sessionId = "test-session-sys-001";
        sessionService = new PCAgentService4Session(
                sessionId, llmInvoker, specParserService, selectionService, configService,
                s -> {});
    }

    /**
     * 测试 doGeneratorConfig - 完整流程（使用真实 LLM）
     * 参考 PCAgentService4SessionTest.testDoGeneratorConfig_CompleteFlow
     * 
     * 注意：此测试需要设置环境变量才能运行
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    @EnableLLMCache(true)
    void shouldUseRealLlmToGenerateConfig_CompleteFlow() {
        // 准备测试数据
        String userInput = """
                我有一名高端客户 深圳国资委，需要建立数据中心，要求如下：
                数据中心服务器 512台
                1. 形态与处理器：2U机架式服务器；配置≥2颗最新一代Intel® Xeon® Scalable处理器，每颗核心数≥16核。
                2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。
                3. 存储：配置≥8块2.4TB 10K RPM SAS硬盘；支持硬件RAID 0, 1, 5, 10，缓存≥4GB。
                """;

        // 执行测试
        sessionService.doGeneratorConfig(userInput);

        // 验证结果
        Session session = sessionService.getCurrentSession();
        assertNotNull(session, "Session不应为null");
        assertEquals("test-session-sys-001", session.getSessionId());
        assertNotNull(session.getProgress(), "Progress不应为null");
        assertEquals(3, session.getProgress().getCurrent(), "进度应该完成");
        assertEquals("配置完成", session.getProgress().getMessage());

        // 验证各步骤的数据
        // Step1: ConfigReq
        assertNotNull(session.getData(), "Session data不应为null");
        
        // 验证最终结果是 ProductConfig
        assertTrue(session.getData() instanceof ProductConfig, 
                "最终数据应该是 ProductConfig");
        
        ProductConfig productConfig = (ProductConfig) session.getData();
        assertNotNull(productConfig.getProductCode(), "产品代码不应为null");
        assertNotNull(productConfig.getParas(), "参数列表不应为null");
        assertTrue(productConfig.getParas().size() > 0, "应该至少有一个参数");
        assertNotNull(productConfig.getCheckResult(), "检查结果不应为null");
    }
 
}

