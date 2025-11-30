package com.pcagent.service;

import com.pcagent.model.*;
import com.pcagent.service.impl.ProductOntoDataSample;
import com.pcagent.service.impl.ProductOntoService4Local;
import com.pcagent.util.ProductOntoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * ProductConfigService 测试类
 */
class ProductConfigServiceTest {
    private ProductConfigService configService;
    private ProductOntoService4Local productOntoService;

    @BeforeEach
    void setUp() {
        productOntoService = new ProductOntoService4Local();
        productOntoService.init();
        configService = new ProductConfigService(productOntoService);
    }

    /**
     * 测试 doParameterConfigs 方法
     * 测试场景：req.totalQuantity = 55，产品为 poweredge_r760xa
     * 规格需求：
     * - CPU:处理器核心数 >= 16核
     * - 内存:内存容量 >= 512GB
     */
    @Test
    void testDoParameterConfigs() throws IOException {
        // 构建 ProductDeviationDegree - 使用JSON构建SpecItemDeviationDegree
        String cpuSpecItemJson = """
            {
              "originalSpecReq": "",
              "specName": "CPU:处理器核心数",
              "satisfy": true,
              "deviationDegree": "POSITIVE",
              "stdSpecReq": {
                "specName": "CPU:处理器核心数",
                "compare": ">=",
                "specValue": "16",
                "unit": "核",
                "type": "INTEGER"
              }
            }
            """;

        String memSpecItemJson = """
            {
              "originalSpecReq": "",
              "specName": "内存:内存容量",
              "satisfy": true,
              "deviationDegree": "POSITIVE",
              "stdSpecReq": {
                "specName": "内存:内存容量",
                "compare": ">=",
                "specValue": "512",
                "unit": "GB",
                "type": "INTEGER"
              }
            }
            """;

        SpecItemDeviationDegree cpuSpecItem = ProductOntoUtils.fromJsonString(
                cpuSpecItemJson.trim(), SpecItemDeviationDegree.class);
        SpecItemDeviationDegree memSpecItem = ProductOntoUtils.fromJsonString(
                memSpecItemJson.trim(), SpecItemDeviationDegree.class);

        // 构建 ProductDeviationDegree
        ProductDeviationDegree productDeviationDegree = new ProductDeviationDegree();
        productDeviationDegree.setProductCode(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA);
        productDeviationDegree.setTotalDeviationDegrees(100);
        // 使用 addSpecItemDeviationDegree 方法，确保 map 被正确填充
        productDeviationDegree.addSpecItemDeviationDegree(cpuSpecItem);
        productDeviationDegree.addSpecItemDeviationDegree(memSpecItem);

        // 构建 ConfigReq
        ConfigReq req = new ConfigReq();
        req.setTotalQuantity(55);

        // 执行测试
        ProductConfig result = configService.doParameterConfigs(productDeviationDegree, req);

        // 验证结果
        org.junit.jupiter.api.Assertions.assertNotNull(result, "ProductConfig不应为null");
        org.junit.jupiter.api.Assertions.assertEquals(
                ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA, 
                result.getProductCode(),
                "productCode应该等于poweredge_r760xa");
        org.junit.jupiter.api.Assertions.assertNotNull(result.getParas(), "paras不应为null");
        org.junit.jupiter.api.Assertions.assertNotNull(result.getCheckResult(), "checkResult不应为null");
        
        // 验证QTY参数应该等于55
        ParameterConfig qtyConfig = result.getParas().stream()
                .filter(p -> "QTY".equals(p.getCode()))
                .findFirst()
                .orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(qtyConfig, "应该包含QTY参数");
        org.junit.jupiter.api.Assertions.assertEquals("55", qtyConfig.getValue(), "QTY应该等于55");
    }
}

