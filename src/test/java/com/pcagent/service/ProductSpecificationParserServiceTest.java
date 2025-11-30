package com.pcagent.service;

import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.model.SpecificationReq;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
    void testParseProductSpecsByCatalogNode() {
        // 准备测试数据
        String catalogNode = "data_center_server";
        List<String> specReqItems = Arrays.asList(
                "内存：配置≥256GB DDR4 ECC Registered内存，并提供≥16个内存插槽以供扩展。"
        );

        // 执行测试
        ProductSpecificationReq result = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);

        // 验证结果
        assertNotNull(result, "解析结果不应为null");
        assertEquals(catalogNode, result.getCatalogNode(), "目录节点应该匹配");
        assertNotNull(result.getSpecReqs(), "规格需求列表不应为null");
        assertFalse(result.getSpecReqs().isEmpty(), "规格需求列表不应为空");
        
        // 验证第一个规格需求
        SpecificationReq firstSpecReq = result.getSpecReqs().get(0);
        assertNotNull(firstSpecReq, "第一个规格需求不应为null");
        assertEquals(specReqItems.get(0), firstSpecReq.getOriginalSpec(), "原始规格应该匹配");
        assertNotNull(firstSpecReq.getStdSpecs(), "标准规格列表不应为null");
    }
}

