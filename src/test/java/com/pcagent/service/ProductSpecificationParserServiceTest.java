package com.pcagent.service;

import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.pcagent.service.ProductSpecificationReqAssert.assertReq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductSpecificationParserService测试类
 */
class ProductSpecificationParserServiceTest {
    private ProductSpecificationParserService parserService;
    private ProductOntoService4Local productOntoService;

    @BeforeEach
    void setUp() {
        productOntoService = new ProductOntoService4Local();
        productOntoService.init();
        parserService = new ProductSpecificationParserService(productOntoService);
    }

    @Test
    void testParseProductSpecsByCatalogNodeOneSpec() {
        // 准备测试数据
        String catalogNode = "data_center_server";
        List<String> specReqItems = Arrays.asList(
                "内存:配置≥256GB DDR4 ECC Registered内存"
        ); 
        // 执行测试
        ProductSpecificationReq result = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);

        // 验证结果
        assertReq(result)
                .catalogNodeEqual("data_center_server")
                .specReqsSize(1)
                .specReq(0)
                .specNameEqual("内存:内存容量")
                .compareEqual(">=")
                .specValueEqual("256")
                .unitEqual("GB");
    }

    @Test
    void testParseProductSpecsByCatalogNodeMultiSpecs() {
        // 准备测试数据
        String catalogNode = "data_center_server";
        List<String> specReqItems = Arrays.asList(
                "内存:配置≥128GB DDR1 ECC Registered内存"
        );

        // Mock规格匹配,支持动态根据匹配规格添加匹配场景
        productOntoService.mockSpecMatch(
                "内存:配置≥128GB DDR1 ECC Registered内存",
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "128","unit": "GB"}
                """,
                """
                {"specName": "内存:集成内存","compare": ">=","specValue": "128","unit": "GB"}
                """
        );

        // 执行测试
        ProductSpecificationReq result = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);

        // 验证结果
        assertReq(result)
                .catalogNodeEqual("data_center_server")
                .specReqsSize(1)
                .specReq(0)
                .stdSpecsSize(2)
                .stdSpec(0)
                .specNameEqual("内存:内存容量")
                .compareEqual(">=")
                .specValueEqual("128")
                .unitEqual("GB");
        
        // 验证第二个规格
        assertReq(result)
                .specReq(0)
                .stdSpec(1)
                .specNameEqual("内存:集成内存")
                .compareEqual(">=")
                .specValueEqual("128")
                .unitEqual("GB");
    }

    @Test
    void testParseProductSpecs() {
        // 准备测试数据
        String productSerial = "center_server";
        List<String> specReqItems = Arrays.asList(
                "CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核 ",
                "内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展"
        );

        // 执行测试
        List<ProductSpecificationReq> results = parserService.parseProductSpecs(productSerial, specReqItems);
  
        // 验证第一个结果（应该匹配到 data_center_server 节点）
        ProductSpecificationReq result = results.get(0);
        assertReq(result)
                .catalogNodeEqual("data_center_server")
                .specReqsSize(2)
                .specReq(0)
                .originalSpecEqual("CPU:最新一代Intel® Xeon® Scalable处理器，核心数≥16核 ")
                .stdSpecsSize(1)
                .stdSpec(0)
                .specNameEqual("CPU:处理器核心数")
                .compareEqual(">=")
                .specValueEqual("16")
                .unitEqual("核");
        
        // 验证第二个规格（内存规格，如果匹配到的话）
        assertReq(result)
                .specReq(1)
                .originalSpecEqual("内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展");
    }
}

