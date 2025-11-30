package com.pcagent.service;

import com.pcagent.model.ProductSpecificationReq;
import com.pcagent.model.Specification;
import com.pcagent.model.SpecificationReq;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * ProductSpecificationReq 断言工具类
 * 提供流式API用于测试断言
 */
public class ProductSpecificationReqAssert {
    private final ProductSpecificationReq actual;

    public ProductSpecificationReqAssert(ProductSpecificationReq actual) {
        this.actual = actual;
    }

    /**
     * 创建断言对象
     */
    public static ProductSpecificationReqAssert assertReq(ProductSpecificationReq actual) {
        Assertions.assertNotNull(actual, "ProductSpecificationReq不应为null");
        return new ProductSpecificationReqAssert(actual);
    }

    /**
     * 断言 catalogNode 等于指定值
     */
    public ProductSpecificationReqAssert catalogNodeEqual(String expected) {
        Assertions.assertEquals(expected, actual.getCatalogNode(), 
            "catalogNode应该等于: " + expected);
        return this;
    }

    /**
     * 断言 specReqs 的大小等于指定值
     */
    public ProductSpecificationReqAssert specReqsSize(int expectedSize) {
        Assertions.assertNotNull(actual.getSpecReqs(), "specReqs不应为null");
        Assertions.assertEquals(expectedSize, actual.getSpecReqs().size(), 
            "specReqs的大小应该为: " + expectedSize);
        return this;
    }

    /**
     * 获取指定索引的 SpecificationReq 断言对象
     */
    public SpecificationReqAssert specReq(int index) {
        Assertions.assertNotNull(actual.getSpecReqs(), "specReqs不应为null");
        Assertions.assertTrue(index >= 0 && index < actual.getSpecReqs().size(), 
            "specReq索引超出范围: " + index);
        SpecificationReq specReq = actual.getSpecReqs().get(index);
        return new SpecificationReqAssert(specReq);
    }

    /**
     * SpecificationReq 断言类
     */
    public static class SpecificationReqAssert {
        private final SpecificationReq actual;
        private Specification currentSpec;

        public SpecificationReqAssert(SpecificationReq actual) {
            this.actual = actual;
        }

        /**
         * 断言 originalSpec 等于指定值
         */
        public SpecificationReqAssert originalSpecEqual(String expected) {
            Assertions.assertEquals(expected, actual.getOriginalSpec(), 
                "originalSpec应该等于: " + expected);
            return this;
        }

        /**
         * 断言 stdSpecs 的大小等于指定值
         */
        public SpecificationReqAssert stdSpecsSize(int expectedSize) {
            Assertions.assertNotNull(actual.getStdSpecs(), "stdSpecs不应为null");
            Assertions.assertEquals(expectedSize, actual.getStdSpecs().size(), 
                "stdSpecs的大小应该为: " + expectedSize);
            return this;
        }

        /**
         * 选择指定索引的 Specification 进行断言
         */
        public SpecificationReqAssert stdSpec(int index) {
            Assertions.assertNotNull(actual.getStdSpecs(), "stdSpecs不应为null");
            Assertions.assertTrue(index >= 0 && index < actual.getStdSpecs().size(), 
                "stdSpec索引超出范围: " + index);
            this.currentSpec = actual.getStdSpecs().get(index);
            return this;
        }

        /**
         * 断言当前 Specification 的 specName 等于指定值
         * 如果没有选择 stdSpec，默认使用第一个
         */
        public SpecificationReqAssert specNameEqual(String expected) {
            Specification spec = getCurrentSpec();
            Assertions.assertEquals(expected, spec.getSpecName(), 
                "specName应该等于: " + expected);
            return this;
        }

        /**
         * 断言当前 Specification 的 compare 等于指定值
         */
        public SpecificationReqAssert compareEqual(String expected) {
            Specification spec = getCurrentSpec();
            Assertions.assertEquals(expected, spec.getCompare(), 
                "compare应该等于: " + expected);
            return this;
        }

        /**
         * 断言当前 Specification 的 specValue 等于指定值
         */
        public SpecificationReqAssert specValueEqual(String expected) {
            Specification spec = getCurrentSpec();
            Assertions.assertEquals(expected, spec.getSpecValue(), 
                "specValue应该等于: " + expected);
            return this;
        }

        /**
         * 断言当前 Specification 的 unit 等于指定值
         */
        public SpecificationReqAssert unitEqual(String expected) {
            Specification spec = getCurrentSpec();
            Assertions.assertEquals(expected, spec.getUnit(), 
                "unit应该等于: " + expected);
            return this;
        }

        /**
         * 获取当前选择的 Specification，如果没有选择则使用第一个
         */
        private Specification getCurrentSpec() {
            if (currentSpec != null) {
                return currentSpec;
            }
            List<Specification> stdSpecs = actual.getStdSpecs();
            Assertions.assertNotNull(stdSpecs, "stdSpecs不应为null");
            Assertions.assertFalse(stdSpecs.isEmpty(), "stdSpecs不应为空");
            return stdSpecs.get(0);
        }
    }
}

