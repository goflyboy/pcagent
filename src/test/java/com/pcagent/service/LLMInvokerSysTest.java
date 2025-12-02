package com.pcagent.service;

import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import com.pcagent.util.ChatModelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigReqRecognitionService 系统测试类
 * 使用真实的 ChatModel（不 mock）
 * 参考 BidParserMain 编写
 * 
 * 注意：需要设置环境变量才能运行：
 * - DEEPSEEK_API_KEY: DeepSeek API key
 * - DEEPSEEK_BASE_URL: DeepSeek API base URL (可选，默认: https://api.deepseek.com)
 * 或
 * - QINWEN_API_KEY: 通义千问 API key
 * - QINWEN_BASE_URL: 通义千问 API base URL (可选，默认: https://dashscope.aliyuncs.com/compatible-mode/v1)
 */
class LLMInvokerSysTest {

    private ConfigReqRecognitionService configReqRecognitionService;

    @BeforeEach
    void setUp() {
        // 尝试创建真实的 ChatModel
        ChatModel chatModel = ChatModelBuilder.createIfAvailable();
        LLMInvoker llmInvoker;
        if (chatModel != null) {
            llmInvoker = new LLMInvoker(chatModel);
        } else {
            // 如果没有可用的 ChatModel，使用无参构造函数（会使用简单解析）
            llmInvoker = new LLMInvoker();
        }
        configReqRecognitionService = new ConfigReqRecognitionService(llmInvoker);
    }

    /**
     * 测试使用真实 LLM 解析配置需求
     * 参考 LLMInvokerTest.shouldUseLlmResponseWhenJsonValid
     * 
     * 注意：此测试需要设置环境变量才能运行
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldUseRealLlmToParseConfigReq() {
        String userInput = """
                我有一名高端客户，需要建立数据中心，要求如下：
                数据中心服务器 512台
                1. 形态与处理器：2U机架式服务器；配置≥2颗最新一代Intel® Xeon® Scalable处理器，每颗核心数≥16核。
                2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。
                3. 存储：配置≥8块2.4TB 10K RPM SAS硬盘；支持硬件RAID 0, 1, 5, 10，缓存≥4GB。
                """;

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        // 验证基本字段不为空
        assertNotNull(result, "ConfigReq不应为null");
        assertNotNull(result.getProductSerial(), "产品系列不应为null");
        assertTrue(result.getTotalQuantity() > 0, "总套数应该大于0");
        assertNotNull(result.getConfigStrategy(), "配置策略不应为null");
        assertNotNull(result.getSpecReqItems(), "规格项列表不应为null");
        assertTrue(result.getSpecReqItems().size() > 0, "应该至少有一个规格项");

        // 验证产品系列在枚举中
        String productSerials = "ONU,电脑,服务器,路由器";
        assertTrue(
                productSerials.contains(result.getProductSerial()),
                "产品系列应该在枚举中: " + result.getProductSerial()
        );

        // 验证数量
        assertEquals(512, result.getTotalQuantity(), "总套数应该是512");

        // 验证规格项数量
        assertTrue(result.getSpecReqItems().size() >= 3, "应该至少解析出3个规格项");
    }

    /**
     * 测试使用真实 LLM 解析简单输入
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void shouldUseRealLlmToParseSimpleInput() {
        String userInput = """
                数据中心服务器 500台
                1. CPU核心数≥16核
                2. 内存≥256GB
                """;

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        assertNotNull(result);
        assertEquals("服务器", result.getProductSerial());
        assertEquals(500, result.getTotalQuantity());
        assertTrue(result.getSpecReqItems().size() >= 2);
    }

    /**
     * 测试使用真实 LLM 解析时，如果 LLM 不可用则使用 fallback
     */
    @Test
    void shouldFallbackToSimpleParserWhenLlmNotAvailable() {
        // 使用无 ChatModel 的 ConfigReqRecognitionService
        LLMInvoker simpleInvoker = new LLMInvoker();
        ConfigReqRecognitionService simpleService = new ConfigReqRecognitionService(simpleInvoker);

        String userInput = """
                数据中心服务器 500台
                1. CPU核心数≥16核
                2. 内存≥256GB
                """;

        ConfigReq result = simpleService.parseConfigReq(userInput);

        // Fallback 应该能解析出基本信息
        assertNotNull(result);
        assertEquals("服务器", result.getProductSerial());
        assertEquals(500, result.getTotalQuantity());
        assertEquals(ConfigStrategy.PRICE_MIN_PRIORITY, result.getConfigStrategy());
        assertTrue(result.getSpecReqItems().size() >= 2);
    }

}

