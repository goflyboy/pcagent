package com.pcagent.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMCache 测试类
 */
class LLMCacheTest {

    @BeforeEach
    void setUp() {
        // 清除缓存，确保测试环境干净
        LLMCache.clear();
    }

    @AfterEach
    void tearDown() {
        // 测试后清除缓存
        LLMCache.clear();
    }

    @Test
    void testCacheDisabledByDefault() {
        // 默认情况下缓存应该是禁用的
        assertFalse(LLMCache.isEnabled(), "默认情况下缓存应该被禁用");
    }

    @Test
    void testCacheEnabledViaSystemProperty() {
        // 通过系统属性启用缓存
        System.setProperty("llm.cache.enabled", "true");
        try {
            assertTrue(LLMCache.isEnabled(), "通过系统属性启用缓存后应该返回 true");
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }

    @Test
    void testCacheEnabledViaEnvironmentVariable() {
        // 注意：这个测试需要环境变量，可能在某些环境中不可用
        // 这里只测试系统属性的情况
        System.setProperty("llm.cache.enabled", "true");
        try {
            assertTrue(LLMCache.isEnabled());
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }

    @Test
    void testCachePutAndGet() {
        // 启用缓存
        System.setProperty("llm.cache.enabled", "true");
        try {
            String prompt = "test prompt";
            String response = "test response";

            // 第一次获取应该返回 null
            assertNull(LLMCache.get(prompt), "缓存应该为空");

            // 保存到缓存
            LLMCache.put(prompt, response);

            // 再次获取应该返回缓存的内容
            String cached = LLMCache.get(prompt);
            assertNotNull(cached, "应该能从缓存中获取内容");
            assertEquals(response, cached, "缓存的内容应该与保存的内容一致");
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }

    @Test
    void testCacheWithDifferentPrompts() {
        System.setProperty("llm.cache.enabled", "true");
        try {
            String prompt1 = "prompt 1";
            String response1 = "response 1";
            String prompt2 = "prompt 2";
            String response2 = "response 2";

            LLMCache.put(prompt1, response1);
            LLMCache.put(prompt2, response2);

            assertEquals(response1, LLMCache.get(prompt1));
            assertEquals(response2, LLMCache.get(prompt2));
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }

    @Test
    void testCacheClear() {
        System.setProperty("llm.cache.enabled", "true");
        try {
            String prompt = "test prompt";
            String response = "test response";

            LLMCache.put(prompt, response);
            assertNotNull(LLMCache.get(prompt));

            LLMCache.clear();
            assertNull(LLMCache.get(prompt), "清除缓存后应该返回 null");
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }

    @Test
    void testCacheWithNullPrompt() {
        System.setProperty("llm.cache.enabled", "true");
        try {
            // null prompt 应该返回 null
            assertNull(LLMCache.get(null));
            assertNull(LLMCache.get(""));

            // null prompt 不应该保存
            LLMCache.put(null, "response");
            LLMCache.put("", "response");
            assertNull(LLMCache.get(null));
            assertNull(LLMCache.get(""));
        } finally {
            System.clearProperty("llm.cache.enabled");
        }
    }
}

