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
}

