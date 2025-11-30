package com.pcagent.service;

import com.pcagent.model.ParameterConfigIntent;
import com.pcagent.model.ParameterConfigIntentOption;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * ParameterConfigIntent 断言工具类
 * 提供流式API用于测试断言
 */
public class ParameterConfigIntentAssert {
    private final ParameterConfigIntent actual;

    public ParameterConfigIntentAssert(ParameterConfigIntent actual) {
        this.actual = actual;
    }

    /**
     * 创建断言对象
     */
    public static ParameterConfigIntentAssert assertIntent(ParameterConfigIntent actual) {
        Assertions.assertNotNull(actual, "ParameterConfigIntent不应为null");
        return new ParameterConfigIntentAssert(actual);
    }

    /**
     * 断言 code 等于指定值
     */
    public ParameterConfigIntentAssert codeEqual(String expected) {
        Assertions.assertEquals(expected, actual.getCode(), 
            "code应该等于: " + expected);
        return this;
    }

    /**
     * 断言 intentOptions 的大小等于指定值
     */
    public ParameterConfigIntentAssert intentOptionsSize(int expectedSize) {
        Assertions.assertNotNull(actual.getIntentOptions(), "intentOptions不应为null");
        Assertions.assertEquals(expectedSize, actual.getIntentOptions().size(), 
            "intentOptions的大小应该为: " + expectedSize);
        return this;
    }

    /**
     * 获取指定索引的 IntentOption 断言对象
     */
    public IntentOptionAssert intentOption(int index) {
        Assertions.assertNotNull(actual.getIntentOptions(), "intentOptions不应为null");
        Assertions.assertTrue(index >= 0 && index < actual.getIntentOptions().size(), 
            "intentOption索引超出范围: " + index);
        ParameterConfigIntentOption option = actual.getIntentOptions().get(index);
        return new IntentOptionAssert(option);
    }

    /**
     * 通过 value 获取 IntentOption 断言对象
     */
    public IntentOptionAssert intentOptionByValue(String value) {
        List<ParameterConfigIntentOption> options = actual.getIntentOptions();
        Assertions.assertNotNull(options, "intentOptions不应为null");
        ParameterConfigIntentOption option = options.stream()
            .filter(o -> value.equals(o.getValue()))
            .findFirst()
            .orElse(null);
        Assertions.assertNotNull(option, "找不到value为: " + value + " 的IntentOption");
        return new IntentOptionAssert(option);
    }

    /**
     * IntentOption 断言类
     */
    public class IntentOptionAssert {
        private final ParameterConfigIntentOption actual;

        public IntentOptionAssert(ParameterConfigIntentOption actual) {
            this.actual = actual;
        }

        /**
         * 断言 value 等于指定值
         */
        public IntentOptionAssert valueEqual(String expected) {
            Assertions.assertEquals(expected, actual.getValue(), 
                "value应该等于: " + expected);
            return this;
        }

        /**
         * 断言 message 等于指定值
         */
        public IntentOptionAssert messageEqual(String expected) {
            Assertions.assertEquals(expected, actual.getMessage(), 
                "message应该等于: " + expected);
            return this;
        }

        /**
         * 断言 isVisited 等于指定值
         */
        public IntentOptionAssert isVisitedEqual(Boolean expected) {
            Assertions.assertEquals(expected, actual.getIsVisited(), 
                "isVisited应该等于: " + expected);
            return this;
        }

        /**
         * 返回到父断言对象，用于链式调用多个选项
         */
        public ParameterConfigIntentAssert and() {
            return ParameterConfigIntentAssert.this;
        }
    }
}

