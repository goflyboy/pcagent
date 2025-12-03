package com.pcagent.testcore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 LLM 缓存的注解
 * 在测试类或测试方法上使用此注解可以启用 LLM 缓存
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @EnableLLMCache
 * class MyTest {
 *     // 测试方法会使用缓存
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableLLMCache {
    /**
     * 是否启用缓存，默认为 true
     */
    boolean value() default true;
}

