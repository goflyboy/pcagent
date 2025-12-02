package com.pcagent.service;

import com.pcagent.model.ConfigReq;
import com.pcagent.model.ConfigStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ConfigReqRecognitionService 测试类
 * 测试配置需求识别服务的解析功能
 * 参考 BidParserTest 编写
 */
class LLMInvokerTest {

    private ChatModel chatModel;
    private LLMInvoker llmInvoker;
    private ConfigReqRecognitionService configReqRecognitionService;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        llmInvoker = new LLMInvoker(chatModel);
        configReqRecognitionService = new ConfigReqRecognitionService(llmInvoker);
    }

    /**
     * 测试 LLM 返回有效 JSON 时使用 LLM 响应
     * 参考 BidParserTest.shouldUseLlmResponseWhenJsonValid
     */
    @Test
    void shouldUseLlmResponseWhenJsonValid() {
        // Mock LLM 返回有效的 JSON
        String llmResponse = """
                {
                  "productSerial": "服务器",
                  "totalQuantity": 512,
                  "specReqItems": [
                    "2U机架式服务器，双路CPU",
                    "256GB DDR4 ECC Registered内存，16个插槽",
                    "8块2.4TB 10K RPM SAS硬盘，RAID 0/1/5/10，缓存≥4GB"
                  ],
                  "configStrategy": "TECH_MAX_PRIORITY",
                  "totalQuantityMemo": ""
                }
                """;

        mockChatModelResponse(llmResponse);

        String userInput = sampleInput();

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        assertEquals("服务器", result.getProductSerial());
        assertEquals(512, result.getTotalQuantity());
        assertEquals(ConfigStrategy.TECH_MAX_PRIORITY, result.getConfigStrategy());
        assertEquals(3, result.getSpecReqItems().size());
        assertEquals("", result.getTotalQuantityMemo());

        // 验证 ChatModel 被调用
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    /**
     * 测试 LLM 返回无效 JSON 时使用 fallback
     * 参考 BidParserTest.shouldFallbackWhenLlmResponseInvalid
     */
    @Test
    void shouldFallbackWhenLlmResponseInvalid() {
        // Mock LLM 返回无效的 JSON
        mockChatModelResponse("not a json");

        String userInput = sampleInput();

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        // Fallback 应该能解析出基本信息
        assertEquals("服务器", result.getProductSerial());
        assertEquals(512, result.getTotalQuantity());
        assertEquals(ConfigStrategy.PRICE_MIN_PRIORITY, result.getConfigStrategy());
        assertTrue(result.getSpecReqItems().size() >= 3, "应该至少解析出3个规格项");
    }

    /**
     * 测试 LLM 返回包含 markdown 代码块的 JSON
     */
    @Test
    void shouldExtractJsonFromMarkdownCodeBlock() {
        // Mock LLM 返回包含 markdown 代码块的响应
        String llmResponse = """
                ```json
                {
                  "productSerial": "服务器",
                  "totalQuantity": 512,
                  "specReqItems": [
                    "2U机架式服务器"
                  ],
                  "configStrategy": "PRICE_MIN_PRIORITY",
                  "totalQuantityMemo": ""
                }
                ```
                """;

        mockChatModelResponse(llmResponse);

        String userInput = sampleInput();

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        assertEquals("服务器", result.getProductSerial());
        assertEquals(512, result.getTotalQuantity());
    }

    /**
     * 测试没有 ChatModel 时使用简单解析
     */
    @Test
    void shouldUseSimpleParserWhenChatModelNotAvailable() {
        // 创建没有 ChatModel 的 ConfigReqRecognitionService
        LLMInvoker simpleInvoker = new LLMInvoker();
        ConfigReqRecognitionService simpleService = new ConfigReqRecognitionService(simpleInvoker);

        String userInput = """
                数据中心服务器 500台
                1. CPU核心数≥16核
                2. 内存≥256GB
                """;

        ConfigReq result = simpleService.parseConfigReq(userInput);

        assertNotNull(result);
        assertEquals("服务器", result.getProductSerial());
        assertEquals(500, result.getTotalQuantity());
        assertEquals(ConfigStrategy.PRICE_MIN_PRIORITY, result.getConfigStrategy());
        assertTrue(result.getSpecReqItems().size() >= 2);
    }

    /**
     * 测试产品系列验证
     */
    @Test
    void shouldValidateProductSerial() {
        // Mock LLM 返回不在枚举中的产品系列
        String llmResponse = """
                {
                  "productSerial": "不存在的产品",
                  "totalQuantity": 100,
                  "specReqItems": [],
                  "configStrategy": "PRICE_MIN_PRIORITY",
                  "totalQuantityMemo": ""
                }
                """;

        mockChatModelResponse(llmResponse);

        String userInput = "需要100台服务器";

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        // 产品系列应该被清空（因为不在枚举中）
        assertEquals("", result.getProductSerial());
    }

    /**
     * 测试数量默认值处理
     */
    @Test
    void shouldSetDefaultQuantityWhenNotSpecified() {
        // Mock LLM 返回没有数量的响应
        String llmResponse = """
                {
                  "productSerial": "服务器",
                  "totalQuantity": 0,
                  "specReqItems": [],
                  "configStrategy": "PRICE_MIN_PRIORITY",
                  "totalQuantityMemo": ""
                }
                """;

        mockChatModelResponse(llmResponse);

        String userInput = "需要服务器";

        ConfigReq result = configReqRecognitionService.parseConfigReq(userInput);

        // 数量应该被设置为1
        assertEquals(1, result.getTotalQuantity());
        assertTrue(result.getTotalQuantityMemo().contains("默认值配置1套"));
    }

    /**
     * Mock ChatModel 响应
     * 这里直接使用真实的 AssistantMessage 实例，避免依赖已删除的 getContent() 方法，
     * LLMInvoker 会通过反射从 Generation 的 output 中提取文本。
     */
    private void mockChatModelResponse(String response) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage(response);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);
    }

    /**
     * 示例输入
     */
    private String sampleInput() {
        return """
                我有一名高端客户，需要建立数据中心，要求如下：
                数据中心服务器 512台
                1. 形态与处理器：2U机架式服务器；配置≥2颗最新一代Intel® Xeon® Scalable处理器，每颗核心数≥16核。
                2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。
                3. 存储：配置≥8块2.4TB 10K RPM SAS硬盘；支持硬件RAID 0, 1, 5, 10，缓存≥4GB。
                """;
    }
}

