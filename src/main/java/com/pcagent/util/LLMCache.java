package com.pcagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * LLM 调用缓存管理器
 * 用于缓存大模型的输入和输出，减少开发过程中的等待时间
 */
@Slf4j
public class LLMCache {
    private static final String CACHE_DIR = ".llm-cache";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 缓存条目
     */
    public static class CacheEntry {
        private String prompt;
        private String response;
        private long timestamp;
        
        public CacheEntry() {
        }
        
        public CacheEntry(String prompt, String response) {
            this.prompt = prompt;
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getPrompt() {
            return prompt;
        }
        
        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
        
        public String getResponse() {
            return response;
        }
        
        public void setResponse(String response) {
            this.response = response;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 获取缓存目录
     */
    private static Path getCacheDir() {
        String userDir = System.getProperty("user.dir");
        Path cacheDir = Paths.get(userDir, CACHE_DIR);
        if (!Files.exists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
                log.debug("Created cache directory: {}", cacheDir);
            } catch (IOException e) {
                log.warn("Failed to create cache directory: {}", cacheDir, e);
            }
        }
        return cacheDir;
    }
    
    /**
     * 计算 prompt 的 hash 值
     */
    private static String hashPrompt(String prompt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash prompt", e);
            // 如果 hash 失败，使用简单的文件名（截断）
            return prompt.substring(0, Math.min(50, prompt.length())).replaceAll("[^a-zA-Z0-9]", "_");
        }
    }
    
    /**
     * 获取缓存文件路径
     */
    private static Path getCacheFile(String prompt) {
        String hash = hashPrompt(prompt);
        return getCacheDir().resolve(hash + ".json");
    }
    
    /**
     * 从缓存中获取响应
     * 
     * @param prompt 提示词
     * @return 缓存的响应，如果不存在则返回 null
     */
    public static String get(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return null;
        }
        
        Path cacheFile = getCacheFile(prompt);
        if (!Files.exists(cacheFile)) {
            return null;
        }
        
        try {
            String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
            CacheEntry entry = objectMapper.readValue(content, CacheEntry.class);
            
            // 验证 prompt 是否匹配（防止 hash 冲突）
            if (!entry.getPrompt().equals(prompt)) {
                log.warn("Cache entry prompt mismatch, ignoring cache");
                return null;
            }
            
            log.debug("Cache hit for prompt hash: {}", hashPrompt(prompt));
            return entry.getResponse();
        } catch (IOException e) {
            log.warn("Failed to read cache file: {}", cacheFile, e);
            return null;
        }
    }
    
    /**
     * 将响应保存到缓存
     * 
     * @param prompt 提示词
     * @param response 响应
     */
    public static void put(String prompt, String response) {
        if (prompt == null || prompt.trim().isEmpty() || response == null) {
            return;
        }
        
        try {
            CacheEntry entry = new CacheEntry(prompt, response);
            Path cacheFile = getCacheFile(prompt);
            String json = objectMapper.writeValueAsString(entry);
            Files.writeString(cacheFile, json, StandardCharsets.UTF_8);
            log.debug("Cached response for prompt hash: {}", hashPrompt(prompt));
        } catch (IOException e) {
            log.warn("Failed to write cache file", e);
        }
    }
    
    /**
     * 清除所有缓存
     */
    public static void clear() {
        Path cacheDir = getCacheDir();
        if (Files.exists(cacheDir)) {
            try {
                Files.walk(cacheDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete cache file: {}", p, e);
                        }
                    });
                log.info("Cleared all LLM cache files");
            } catch (IOException e) {
                log.error("Failed to clear cache", e);
            }
        }
    }
    
    /**
     * 检查是否启用缓存
     * 通过系统属性 llm.cache.enabled 控制，默认为 false
     */
    public static boolean isEnabled() {
        String enabled = System.getProperty("llm.cache.enabled");
        if (enabled == null) {
            // 检查环境变量
            enabled = System.getenv("LLM_CACHE_ENABLED");
        }
        return "true".equalsIgnoreCase(enabled);
    }
}

