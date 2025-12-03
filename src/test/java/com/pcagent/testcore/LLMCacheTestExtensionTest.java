package com.pcagent.testcore;

import com.pcagent.util.LLMCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMCacheTestExtension 测试类
 * 验证扩展是否能正确处理类级别和方法级别的注解
 */
@ExtendWith(LLMCacheTestExtension.class)
class LLMCacheTestExtensionTest {

    // 注意：不要在 @BeforeEach 中清除系统属性，因为扩展的 beforeEach 会在 @BeforeEach 之前执行
    // 如果在这里清除，会覆盖扩展设置的属性
    // @AfterEach 中也不需要清除，因为扩展的 afterEach 会恢复原始设置

    /**
     * 测试：没有注解时，默认不启用缓存
     */
    @Test
    void testNoAnnotation_ShouldDisableCache() {
        assertFalse(LLMCache.isEnabled(), "没有注解时应该禁用缓存");
    }

    /**
     * 测试：类级别启用缓存
     */
    @Test
    @EnableLLMCache(true)
    void testClassLevelEnable_ShouldEnableCache() {
        assertTrue(LLMCache.isEnabled(), "类级别启用缓存时应该返回 true");
    }

    /**
     * 测试：类级别禁用缓存
     */
    @Test
    @EnableLLMCache(false)
    void testClassLevelDisable_ShouldDisableCache() {
        assertFalse(LLMCache.isEnabled(), "类级别禁用缓存时应该返回 false");
    }

    /**
     * 测试：方法级别注解覆盖类级别注解
     */
    @ExtendWith(LLMCacheTestExtension.class)
    @EnableLLMCache(true) // 类级别启用
    static class ClassLevelEnabled {
        @Test
        @EnableLLMCache(false) // 方法级别禁用，应该覆盖类级别
        void testMethodLevelOverride_ShouldDisableCache() {
            assertFalse(LLMCache.isEnabled(), 
                "方法级别的 @EnableLLMCache(false) 应该覆盖类级别的 @EnableLLMCache(true)");
        }
    }

    /**
     * 测试：方法级别注解覆盖类级别注解（相反情况）
     */
    @ExtendWith(LLMCacheTestExtension.class)
    @EnableLLMCache(false) // 类级别禁用
    static class ClassLevelDisabled {
        @Test
        @EnableLLMCache(true) // 方法级别启用，应该覆盖类级别
        void testMethodLevelOverride_ShouldEnableCache() {
            assertTrue(LLMCache.isEnabled(), 
                "方法级别的 @EnableLLMCache(true) 应该覆盖类级别的 @EnableLLMCache(false)");
        }
    }
}

