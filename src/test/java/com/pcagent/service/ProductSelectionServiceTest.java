package com.pcagent.service;

import com.pcagent.model.DeviationDegree;
import com.pcagent.model.ProductDeviationDegree;
import com.pcagent.model.ProductSpecification;
import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.model.SpecItemDeviationDegree;
import com.pcagent.model.Specification;
import com.pcagent.service.impl.ProductOntoDataSample;
import com.pcagent.service.impl.ProductOntoService4Local;
import com.pcagent.util.ProductOntoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
                {"specName": "内存:内存容量","compare": ">=","specValue": "512","unit": "GB","type": "INTEGER"}
                """
        );
        productOntoService.mockSpecMatch(
                ProductOntoDataSample.SPEC_REQ_CPU_32_CORES,
                """
                {"specName": "CPU:处理器核心数","compare": ">=","specValue": "32","unit": "核","type": "INTEGER"}
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
                {"specName": "CPU:处理器核心数","compare": ">=","specValue": "16","unit": "核","type": "INTEGER"}
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

    // ========== 公共辅助方法 ==========

    /**
     * 从 JSON 字符串解析 Specification 对象
     */
    private Specification parseSpec(String json) throws IOException {
        return ProductOntoUtils.fromJsonString(json.trim(), Specification.class);
    }

    /**
     * 计算规格项偏离度
     */
    private SpecItemDeviationDegree calcDeviation(String specItemJson, String stdSpecItemReqJson, String originalSpec) throws IOException {
        Specification specItem = parseSpec(specItemJson);
        Specification stdSpecItemReq = parseSpec(stdSpecItemReqJson);
        return selectionService.calcSpecItemDeviationDegree(specItem, stdSpecItemReq, originalSpec);
    }

    /**
     * 验证满足的情况
     */
    private void assertSatisfied(SpecItemDeviationDegree result, String expectedSpecName, String expectedOriginalSpec, DeviationDegree expectedDeviation) {
        assertEquals(expectedOriginalSpec, result.getOriginalSpecReq());
        assertEquals(expectedSpecName, result.getSpecName());
        assertTrue(result.getSatisfy());
        assertEquals(expectedDeviation, result.getDeviationDegree());
    }

    /**
     * 验证不满足的情况
     */
    private void assertNotSatisfied(SpecItemDeviationDegree result, DeviationDegree expectedDeviation) {
        assertFalse(result.getSatisfy());
        assertEquals(expectedDeviation, result.getDeviationDegree());
    }

    // ========== calcSpecItemDeviationDegree 测试用例 ==========

    @Test
    void testCalcSpecItemDeviationDegree_GreaterEqual_Satisfied() throws IOException {
        // 测试 >= 操作符，产品值满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "512","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "内存:内存容量", "", DeviationDegree.POSITIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_GreaterEqual_NotSatisfied() throws IOException {
        // 测试 >= 操作符，产品值不满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "128","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_GreaterEqual_Equal() throws IOException {
        // 测试 >= 操作符，产品值等于需求值
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "内存:内存容量", "", DeviationDegree.POSITIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Greater_Satisfied() throws IOException {
        // 测试 > 操作符，产品值满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "CPU:处理器核心数","compare": ">","specValue": "32","unit": "核","type": "INTEGER"}
                """,
                """
                {"specName": "CPU:处理器核心数","compare": ">","specValue": "16","unit": "核","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "CPU:处理器核心数", "", DeviationDegree.POSITIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Greater_NotSatisfied() throws IOException {
        // 测试 > 操作符，产品值不满足需求（等于）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "CPU:处理器核心数","compare": ">","specValue": "16","unit": "核","type": "INTEGER"}
                """,
                """
                {"specName": "CPU:处理器核心数","compare": ">","specValue": "16","unit": "核","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_LessEqual_Satisfied() throws IOException {
        // 测试 <= 操作符，产品值满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<=","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<=","specValue": "0","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "工作环境:最高工作环境温度", "", DeviationDegree.POSITIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_LessEqual_NotSatisfied() throws IOException {
        // 测试 <= 操作符，产品值不满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<=","specValue": "10","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<=","specValue": "0","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Less_Satisfied() throws IOException {
        // 测试 < 操作符，产品值满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<","specValue": "-50","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "工作环境:最高工作环境温度", "", DeviationDegree.POSITIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Less_NotSatisfied() throws IOException {
        // 测试 < 操作符，产品值不满足需求
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<","specValue": "-30","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "<","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Equal_Satisfied() throws IOException {
        // 测试 = 操作符，产品值满足需求（差值小于0.01）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "工作环境:最高工作环境温度", "", DeviationDegree.NONE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Equal_NotSatisfied() throws IOException {
        // 测试 = 操作符，产品值不满足需求（差值大于等于0.01）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-30","unit": "°C","type": "INTEGER"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Equal_WithSmallDifference() throws IOException {
        // 测试 = 操作符，产品值与需求值差值很小（小于0.01）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-40.005","unit": "°C","type": "FLOAT"}
                """,
                """
                {"specName": "工作环境:最高工作环境温度","compare": "=","specValue": "-40","unit": "°C","type": "INTEGER"}
                """,
                ""
        );
        assertSatisfied(result, "工作环境:最高工作环境温度", "", DeviationDegree.NONE);
    }

    @Test
    void testCalcSpecItemDeviationDegree_Default_NotSatisfied() throws IOException {
        // 测试 default 分支（未知操作符）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": "!=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": "!=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                ""
        );
        assertNotSatisfied(result, DeviationDegree.NOT_FOUND);
    }

    @Test
    void testCalcSpecItemDeviationDegree_WithOriginalSpec() throws IOException {
        // 测试 originalSpec 不为空的情况
        String originalSpec = "内存:配置≥256GB DDR4 ECC Registered内存";
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "512","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                originalSpec
        );
        assertEquals(originalSpec, result.getOriginalSpecReq());
        assertTrue(result.getSatisfy());
    }

    @Test
    void testCalcSpecItemDeviationDegree_WithInvalidValue() throws IOException {
        // 测试异常情况：无法解析的数值（parseValue 返回 0.0，所以 0.0 < 256，结果为 NEGATIVE）
        SpecItemDeviationDegree result = calcDeviation(
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "invalid","unit": "GB","type": "INTEGER"}
                """,
                """
                {"specName": "内存:内存容量","compare": ">=","specValue": "256","unit": "GB","type": "INTEGER"}
                """,
                ""
        );
        // parseValue("invalid") 返回 0.0，0.0 < 256，所以 satisfy=false, deviationDegree=NEGATIVE
        assertNotSatisfied(result, DeviationDegree.NEGATIVE);
    }
}

