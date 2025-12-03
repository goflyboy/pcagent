package com.pcagent.testcore;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

/**
 * JUnit 5 扩展，用于在测试中启用/禁用 LLM 缓存
 * 
 * 使用方式：
 * <pre>
 * {@code
 * @ExtendWith(LLMCacheTestExtension.class)
 * @EnableLLMCache
 * class MyTest {
 *     // 测试会使用缓存
 * }
 * }
 * </pre>
 */
public class LLMCacheTestExtension implements BeforeEachCallback, AfterEachCallback {
    
    private static final String ORIGINAL_CACHE_ENABLED = "llm.cache.enabled.original";
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // 保存原始的缓存设置
        String original = System.getProperty("llm.cache.enabled");
        context.getStore(ExtensionContext.Namespace.create(getClass()))
               .put(ORIGINAL_CACHE_ENABLED, original);
        
        // 检查类级别和方法级别的注解
        Boolean enableCache = null; // 使用 null 表示没有找到注解
        
        // 检查类级别的注解
        EnableLLMCache classAnnotation = context.getRequiredTestClass()
            .getAnnotation(EnableLLMCache.class);
        if (classAnnotation != null) {
            enableCache = classAnnotation.value();
        }
        
        // 检查方法级别的注解（方法级别优先级更高，会覆盖类级别的设置）
        Method testMethod = context.getRequiredTestMethod();
        EnableLLMCache methodAnnotation = testMethod.getAnnotation(EnableLLMCache.class);
        if (methodAnnotation != null) {
            enableCache = methodAnnotation.value();
        }
        
        // 设置系统属性
        if (enableCache != null) {
            // 如果找到了注解（类级别或方法级别），使用注解的值
            System.setProperty("llm.cache.enabled", String.valueOf(enableCache));
        } else {
            // 如果没有找到任何注解，默认不启用缓存
            System.setProperty("llm.cache.enabled", "false");
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // 恢复原始的缓存设置
        String original = context.getStore(ExtensionContext.Namespace.create(getClass()))
                                 .get(ORIGINAL_CACHE_ENABLED, String.class);
        if (original != null) {
            System.setProperty("llm.cache.enabled", original);
        } else {
            System.clearProperty("llm.cache.enabled");
        }
    }
}

