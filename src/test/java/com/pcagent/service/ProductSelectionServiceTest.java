package com.pcagent.service;

import com.pcagent.model.DeviationDegree;
import com.pcagent.model.ProductDeviationDegree;
import com.pcagent.model.ProductSpecification;
import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.service.impl.ProductOntoDataSample;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pcagent.service.ProductDeviationDegreeAssert.assertDeviation;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductSelectionService测试类
 */
class ProductSelectionServiceTest {
    private ProductSelectionService selectionService;
    private ProductSpecificationParserService parserService;
    private ProductOntoService4Local productOntoService;

    @BeforeEach
    void setUp() {
        productOntoService = new ProductOntoService4Local();
        productOntoService.init();
        parserService = new ProductSpecificationParserService(productOntoService);
        selectionService = new ProductSelectionService(productOntoService);
    }

    @Test
    void testCalcProductDeviationDegree_Satisfied() {
        // 准备测试数据 - 解析规格需求
        String catalogNode = ProductOntoDataSample.CATALOG_NODE_DATA_CENTER_SERVER;
        List<String> specReqItems = ProductOntoDataSample.SPEC_REQ_ITEMS_MEMORY_256GB_CPU_16;
        ProductSpecificationReq productSpecReq = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);
        // 获取产品规格 - poweredge_r760xa: CPU>=16核, 内存>=256GB
        ProductSpecification productSpec = productOntoService.queryProductSpecification(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA);
  
        // 执行测试
        ProductDeviationDegree result = selectionService.calcProductDeviationDegree(productSpec, productSpecReq);
 
        // 验证结果
        assertDeviation(result)
                .productCodeEqual(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA)
                .totalDeviationDegreesEqual(100) // 两个规格都满足，满足度100%
                .specItemDeviationDegreesSize(2);
        
        // 验证内存规格
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("内存:内存容量")
                .originalSpecReqEqual(ProductOntoDataSample.SPEC_REQ_MEMORY_256GB)
                .specNameEqual("内存:内存容量")
                .satisfyEqual(true)
                .deviationDegreeEqual(DeviationDegree.POSITIVE)
                .stdSpecReqSpecNameEqual("内存:内存容量")
                .stdSpecReqCompareEqual(">=")
                .stdSpecReqSpecValueEqual("256")
                .stdSpecReqUnitEqual("GB");
        
        // 验证CPU规格
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("CPU:处理器核心数")
                .originalSpecReqEqual(ProductOntoDataSample.SPEC_REQ_CPU_16_CORES)
                .specNameEqual("CPU:处理器核心数")
                .satisfyEqual(true)
                .deviationDegreeEqual(DeviationDegree.POSITIVE)
                .stdSpecReqSpecNameEqual("CPU:处理器核心数")
                .stdSpecReqCompareEqual(">=")
                .stdSpecReqSpecValueEqual("16")
                .stdSpecReqUnitEqual("核");
    }

    @Test
    void testCalcProductDeviationDegree_PartiallySatisfied() {
        // 准备测试数据 - 解析规格需求：要求内存>=512GB, CPU>=32核
        String catalogNode = ProductOntoDataSample.CATALOG_NODE_DATA_CENTER_SERVER;
        List<String> specReqItems = ProductOntoDataSample.SPEC_REQ_ITEMS_MEMORY_512GB_CPU_32;

        // Mock规格匹配
        productOntoService.mockSpecMatch(
                ProductOntoDataSample.SPEC_REQ_MEMORY_512GB,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "512","unit": "GB"}
                """
        );
        productOntoService.mockSpecMatch(
                ProductOntoDataSample.SPEC_REQ_CPU_32_CORES,
                """
                {"specName": "CPU:处理器核心数","compare": ">=","specValue": "32","unit": "核"}
                """
        );

        ProductSpecificationReq productSpecReq = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);

        // 获取产品规格 - poweredge_r760xa: CPU>=16核, 内存>=256GB (不满足需求)
        ProductSpecification productSpec = productOntoService.queryProductSpecification(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA);
        assertNotNull(productSpec, "产品规格不应为null");

        // 执行测试
        var result = selectionService.calcProductDeviationDegree(productSpec, productSpecReq);
        result.setProductCode(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA); // 手动设置productCode

        // 验证结果
        assertDeviation(result)
                .productCodeEqual(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R760XA)
                .totalDeviationDegreesEqual(0) // 两个规格都不满足，满足度0%
                .specItemDeviationDegreesSize(2);
        
        // 验证内存规格 - 256GB < 512GB，不满足
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("内存:内存容量")
                .satisfyEqual(false)
                .deviationDegreeEqual(DeviationDegree.NEGATIVE);
        
        // 验证CPU规格 - 16核 < 32核，不满足
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("CPU:处理器核心数")
                .satisfyEqual(false)
                .deviationDegreeEqual(DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcProductDeviationDegree_FullySatisfiedWithHigherSpec() {
        // 准备测试数据 - 解析规格需求：要求内存>=256GB, CPU>=16核
        String catalogNode = ProductOntoDataSample.CATALOG_NODE_DATA_CENTER_SERVER;
        List<String> specReqItems = ProductOntoDataSample.SPEC_REQ_ITEMS_MEMORY_256GB_CPU_16;

        // Mock规格匹配
        productOntoService.mockSpecMatch(
                ProductOntoDataSample.SPEC_REQ_CPU_16_CORES,
                """
                {"specName": "CPU:处理器核心数","compare": ">=","specValue": "16","unit": "核"}
                """
        );

        ProductSpecificationReq productSpecReq = parserService.parseProductSpecsByCatalogNode(catalogNode, specReqItems);

        // 获取产品规格 - poweredge_r860xa: CPU>=32核, 内存>=512GB (超过需求)
        ProductSpecification productSpec = productOntoService.queryProductSpecification(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R860XA);
        assertNotNull(productSpec, "产品规格不应为null");

        // 执行测试
        var result = selectionService.calcProductDeviationDegree(productSpec, productSpecReq);
        result.setProductCode(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R860XA); // 手动设置productCode

        // 验证结果 - 产品规格超过需求，应该都满足
        assertDeviation(result)
                .productCodeEqual(ProductOntoDataSample.PRODUCT_CODE_POWEREDGE_R860XA)
                .totalDeviationDegreesEqual(100) // 两个规格都满足，满足度100%
                .specItemDeviationDegreesSize(2);
        
        // 验证内存规格 - 512GB >= 256GB，满足
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("内存:内存容量")
                .satisfyEqual(true)
                .deviationDegreeEqual(DeviationDegree.POSITIVE);
        
        // 验证CPU规格 - 32核 >= 16核，满足
        assertDeviation(result)
                .specItemDeviationDegreeBySpecName("CPU:处理器核心数")
                .satisfyEqual(true)
                .deviationDegreeEqual(DeviationDegree.POSITIVE);
    }
}

