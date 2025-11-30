package com.pcagent.service;

import com.pcagent.model.*;
import com.pcagent.service.impl.ProductOntoDataSample;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PCAgentService4Session 测试类
 * 参考 BidParserTest 编写
 */
class PCAgentService4SessionTest {

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

        // Mock LLMInvoker
        llmInvoker = mock(LLMInvoker.class);

        // Mock ProductSpecificationParserService
        specParserService = mock(ProductSpecificationParserService.class);

        // 使用真实的 ProductSelectionService（需要 ProductOntoService）
        selectionService = new ProductSelectionService(productOntoService);

        // 使用真实的 ProductConfigService（需要 ProductOntoService）
        configService = new ProductConfigService(productOntoService);

        // 创建会话服务
        String sessionId = "test-session-001";
        sessionService = new PCAgentService4Session(
                sessionId, llmInvoker, specParserService, selectionService, configService);
    }

    /**
     * 测试 doGeneratorConfig - 完整流程
     * 参考 BidParserTest.shouldUseLlmResponseWhenJsonValid
     */
    @Test
    void testDoGeneratorConfig_CompleteFlow() {
        // 准备测试数据
        String userInput = """
                我有一名高端客户，需要建立数据中心，要求如下：
                数据中心服务器 512台
                1. 形态与处理器：2U机架式服务器；配置≥2颗最新一代Intel® Xeon® Scalable处理器，每颗核心数≥16核。
                2. 内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。
                3. 存储：配置≥8块2.4TB 10K RPM SAS硬盘；支持硬件RAID 0, 1, 5, 10，缓存≥4GB。
                """;

        // Mock LLMInvoker 返回有效的 ConfigReq
        ConfigReq mockConfigReq = new ConfigReq();
        mockConfigReq.setProductSerial("服务器");
        mockConfigReq.setTotalQuantity(512);
        mockConfigReq.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        mockConfigReq.setTotalQuantityMemo("");
        mockConfigReq.setSpecReqItems(List.of(
                "2U机架式服务器，双路CPU，每颗核心数≥16核",
                "256GB DDR4 ECC Registered内存，16个插槽",
                "8块2.4TB 10K RPM SAS硬盘，RAID 0/1/5/10，缓存≥4GB"
        ));

        when(llmInvoker.parseConfigReq(anyString(), anyString())).thenReturn(mockConfigReq);

        // Mock ProductSpecificationParserService
        ProductSpecificationReq mockSpecReq = createMockProductSpecificationReq();
        when(specParserService.parseProductSpecs(anyString(), anyList()))
                .thenReturn(List.of(mockSpecReq));

        // 执行测试
        sessionService.doGeneratorConfig(userInput);

        // 验证结果
        Session session = sessionService.getCurrentSession();
        assertNotNull(session, "Session不应为null");
        assertEquals("test-session-001", session.getSessionId());
        assertNotNull(session.getProgress(), "Progress不应为null");
        assertEquals(3, session.getProgress().getCurrent(), "进度应该完成");
        assertEquals("配置完成", session.getProgress().getMessage());

        // 验证各步骤的数据
        // Step1: ConfigReq
        assertNotNull(session.getData(), "Session data不应为null");

        // 验证 LLMInvoker 被调用
        verify(llmInvoker, times(1)).parseConfigReq(anyString(), anyString());

        // 验证 specParserService 被调用
        verify(specParserService, times(1)).parseProductSpecs(anyString(), anyList());
    }

    /**
     * 测试 doGeneratorConfig - LLM 返回无效数据时使用 fallback
     * 参考 BidParserTest.shouldFallbackWhenLlmResponseInvalid
     */
    @Test
    void testDoGeneratorConfig_WithFallback() {
        // 准备测试数据
        String userInput = """
                数据中心服务器 500台
                1. CPU核心数≥16核
                2. 内存≥256GB
                """;

        // Mock LLMInvoker 返回简单解析的结果（fallback）
        ConfigReq mockConfigReq = new ConfigReq();
        mockConfigReq.setProductSerial("服务器");
        mockConfigReq.setTotalQuantity(500);
        mockConfigReq.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        mockConfigReq.setTotalQuantityMemo("");
        mockConfigReq.setSpecReqItems(List.of(
                "CPU核心数≥16核",
                "内存≥256GB"
        ));

        when(llmInvoker.parseConfigReq(anyString(), anyString())).thenReturn(mockConfigReq);

        // Mock ProductSpecificationParserService
        ProductSpecificationReq mockSpecReq = createMockProductSpecificationReq();
        when(specParserService.parseProductSpecs(anyString(), anyList()))
                .thenReturn(List.of(mockSpecReq));

        // 执行测试
        sessionService.doGeneratorConfig(userInput);

        // 验证结果 - 流程应该完成，最后一步是 ProductConfig
        Session session = sessionService.getCurrentSession();
        assertNotNull(session);
        assertNotNull(session.getData());
        assertEquals(3, session.getProgress().getCurrent(), "进度应该完成");
        assertEquals("配置完成", session.getProgress().getMessage());
        
        // 验证最终结果是 ProductConfig
        assertTrue(session.getData() instanceof ProductConfig, 
                "最终数据应该是 ProductConfig");
    }

    /**
     * 测试 doGeneratorConfig - 无效输入抛出异常
     */
    @Test
    void testDoGeneratorConfig_InvalidInput() {
        // 准备测试数据 - 缺少产品系列
        String userInput = "需要500台服务器";

        // Mock LLMInvoker 返回无效的 ConfigReq（缺少产品系列）
        ConfigReq mockConfigReq = new ConfigReq();
        mockConfigReq.setProductSerial(""); // 空的产品系列
        mockConfigReq.setTotalQuantity(500);
        mockConfigReq.setConfigStrategy(ConfigStrategy.PRICE_MIN_PRIORITY);
        mockConfigReq.setSpecReqItems(new ArrayList<>());

        when(llmInvoker.parseConfigReq(anyString(), anyString())).thenReturn(mockConfigReq);

        // 执行测试并验证异常
        assertThrows(Exception.class, () -> {
            sessionService.doGeneratorConfig(userInput);
        }, "应该抛出异常，因为产品系列为空");
    }

    /**
     * 创建 Mock ProductSpecificationReq
     */
    private ProductSpecificationReq createMockProductSpecificationReq() {
        ProductSpecificationReq specReq = new ProductSpecificationReq();
        specReq.setCatalogNode(ProductOntoDataSample.CATALOG_NODE_DATA_CENTER_SERVER);

        // 创建规格需求
        SpecificationReq req1 = new SpecificationReq();
        req1.setOriginalSpec("CPU核心数≥16核");
        Specification cpuSpec = new Specification();
        cpuSpec.setSpecName("CPU:处理器核心数");
        cpuSpec.setCompare(">=");
        cpuSpec.setSpecValue("16");
        cpuSpec.setUnit("核");
        cpuSpec.setType(ParameterType.INTEGER);
        req1.setStdSpecs(List.of(cpuSpec));
        specReq.getSpecReqs().add(req1);

        SpecificationReq req2 = new SpecificationReq();
        req2.setOriginalSpec("内存≥256GB");
        Specification memSpec = new Specification();
        memSpec.setSpecName("内存:内存容量");
        memSpec.setCompare(">=");
        memSpec.setSpecValue("256");
        memSpec.setUnit("GB");
        memSpec.setType(ParameterType.INTEGER);
        req2.setStdSpecs(List.of(memSpec));
        specReq.getSpecReqs().add(req2);

        return specReq;
    }
}

