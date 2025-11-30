package com.pcagent.service;

import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.pcagent.service.ProductSpecificationReqAssert.assertReq;

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
    void testParseProductSpecsByCatalogNode() {
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
    void testTwoSpection() {
        // 准备测试数据
        String catalogNode = "data_center_server";
        List<String> specReqItems = Arrays.asList(
                "内存:配置≥128GB DDR1 ECC Registered内存"
        );

        // Mock规格匹配
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
}

