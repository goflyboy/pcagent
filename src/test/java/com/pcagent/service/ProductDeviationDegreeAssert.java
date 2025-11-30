package com.pcagent.service;

import com.pcagent.model.DeviationDegree;
import com.pcagent.model.ProductDeviationDegree;
import com.pcagent.model.SpecItemDeviationDegree;
import org.junit.jupiter.api.Assertions;

/**
 * ProductDeviationDegree 断言工具类
 * 提供流式API用于测试断言
 */
public class ProductDeviationDegreeAssert {
    private final ProductDeviationDegree actual;

    public ProductDeviationDegreeAssert(ProductDeviationDegree actual) {
        this.actual = actual;
    }

    /**
     * 创建断言对象
     */
    public static ProductDeviationDegreeAssert assertDeviation(ProductDeviationDegree actual) {
        Assertions.assertNotNull(actual, "ProductDeviationDegree不应为null");
        return new ProductDeviationDegreeAssert(actual);
    }

    /**
     * 断言 productCode 等于指定值
     */
    public ProductDeviationDegreeAssert productCodeEqual(String expected) {
        Assertions.assertEquals(expected, actual.getProductCode(), 
            "productCode应该等于: " + expected);
        return this;
    }

    /**
     * 断言 totalDeviationDegrees 等于指定值
     */
    public ProductDeviationDegreeAssert totalDeviationDegreesEqual(int expected) {
        Assertions.assertEquals(expected, actual.getTotalDeviationDegrees(), 
            "totalDeviationDegrees应该等于: " + expected);
        return this;
    }

    /**
     * 断言 specItemDeviationDegrees 的大小等于指定值
     */
    public ProductDeviationDegreeAssert specItemDeviationDegreesSize(int expectedSize) {
        Assertions.assertNotNull(actual.getSpecItemDeviationDegrees(), "specItemDeviationDegrees不应为null");
        Assertions.assertEquals(expectedSize, actual.getSpecItemDeviationDegrees().size(), 
            "specItemDeviationDegrees的大小应该为: " + expectedSize);
        return this;
    }

    /**
     * 获取指定索引的 SpecItemDeviationDegree 断言对象
     */
    public SpecItemDeviationDegreeAssert specItemDeviationDegree(int index) {
        Assertions.assertNotNull(actual.getSpecItemDeviationDegrees(), "specItemDeviationDegrees不应为null");
        Assertions.assertTrue(index >= 0 && index < actual.getSpecItemDeviationDegrees().size(), 
            "specItemDeviationDegree索引超出范围: " + index);
        SpecItemDeviationDegree specItem = actual.getSpecItemDeviationDegrees().get(index);
        return new SpecItemDeviationDegreeAssert(specItem);
    }

    /**
     * 通过 specName 获取 SpecItemDeviationDegree 断言对象
     */
    public SpecItemDeviationDegreeAssert specItemDeviationDegreeBySpecName(String specName) {
        SpecItemDeviationDegree specItem = actual.querySpecItemDeviationDegree(specName);
        Assertions.assertNotNull(specItem, "找不到specName为: " + specName + " 的SpecItemDeviationDegree");
        return new SpecItemDeviationDegreeAssert(specItem);
    }

    /**
     * SpecItemDeviationDegree 断言类
     */
    public static class SpecItemDeviationDegreeAssert {
        private final SpecItemDeviationDegree actual;

        public SpecItemDeviationDegreeAssert(SpecItemDeviationDegree actual) {
            this.actual = actual;
        }

        /**
         * 断言 originalSpecReq 等于指定值
         */
        public SpecItemDeviationDegreeAssert originalSpecReqEqual(String expected) {
            Assertions.assertEquals(expected, actual.getOriginalSpecReq(), 
                "originalSpecReq应该等于: " + expected);
            return this;
        }

        /**
         * 断言 specName 等于指定值
         */
        public SpecItemDeviationDegreeAssert specNameEqual(String expected) {
            Assertions.assertEquals(expected, actual.getSpecName(), 
                "specName应该等于: " + expected);
            return this;
        }

        /**
         * 断言 satisfy 等于指定值
         */
        public SpecItemDeviationDegreeAssert satisfyEqual(boolean expected) {
            Assertions.assertEquals(expected, actual.getSatisfy(), 
                "satisfy应该等于: " + expected);
            return this;
        }

        /**
         * 断言 deviationDegree 等于指定值
         */
        public SpecItemDeviationDegreeAssert deviationDegreeEqual(DeviationDegree expected) {
            Assertions.assertEquals(expected, actual.getDeviationDegree(), 
                "deviationDegree应该等于: " + expected);
            return this;
        }

        /**
         * 断言 stdSpecReq 的 specName 等于指定值
         */
        public SpecItemDeviationDegreeAssert stdSpecReqSpecNameEqual(String expected) {
            Assertions.assertNotNull(actual.getStdSpecReq(), "stdSpecReq不应为null");
            Assertions.assertEquals(expected, actual.getStdSpecReq().getSpecName(), 
                "stdSpecReq.specName应该等于: " + expected);
            return this;
        }

        /**
         * 断言 stdSpecReq 的 compare 等于指定值
         */
        public SpecItemDeviationDegreeAssert stdSpecReqCompareEqual(String expected) {
            Assertions.assertNotNull(actual.getStdSpecReq(), "stdSpecReq不应为null");
            Assertions.assertEquals(expected, actual.getStdSpecReq().getCompare(), 
                "stdSpecReq.compare应该等于: " + expected);
            return this;
        }

        /**
         * 断言 stdSpecReq 的 specValue 等于指定值
         */
        public SpecItemDeviationDegreeAssert stdSpecReqSpecValueEqual(String expected) {
            Assertions.assertNotNull(actual.getStdSpecReq(), "stdSpecReq不应为null");
            Assertions.assertEquals(expected, actual.getStdSpecReq().getSpecValue(), 
                "stdSpecReq.specValue应该等于: " + expected);
            return this;
        }

        /**
         * 断言 stdSpecReq 的 unit 等于指定值
         */
        public SpecItemDeviationDegreeAssert stdSpecReqUnitEqual(String expected) {
            Assertions.assertNotNull(actual.getStdSpecReq(), "stdSpecReq不应为null");
            Assertions.assertEquals(expected, actual.getStdSpecReq().getUnit(), 
                "stdSpecReq.unit应该等于: " + expected);
            return this;
        }
    }
}

