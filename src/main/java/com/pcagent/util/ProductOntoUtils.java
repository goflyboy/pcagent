package com.pcagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 产品本体工具类
 * 提供产品本体对象的JSON序列化和反序列化功能
 * 参考ModuleUtils实现
 * 
 * @since 2025-11-28
 */
@Slf4j
public final class ProductOntoUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 配置ObjectMapper
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT); // 美化输出
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // 允许空Bean序列化
    }

    /**
     * 私有构造器，防止工具类被实例化
     */
    private ProductOntoUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 将对象序列化为JSON文件
     * 
     * @param obj         对象
     * @param jsonFileName JSON文件名（包含路径）
     * @throws IOException 如果文件操作失败
     */
    public static void toJsonFile(Object obj, String jsonFileName) throws IOException {
        log.info("Serializing object to JSON file: {}", jsonFileName);

        // 确保目录存在
        Path path = Paths.get(jsonFileName);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // 序列化为JSON文件
        OBJECT_MAPPER.writeValue(new File(jsonFileName), obj);

        log.info("Successfully serialized object to JSON file: {}", jsonFileName);
    }

    /**
     * 从JSON文件反序列化为对象
     * 
     * @param jsonFileName JSON文件名（包含路径）
     * @param clazz        目标类
     * @return 对象
     * @throws IOException 如果文件操作失败
     */
    public static <T> T fromJsonFile(String jsonFileName, Class<T> clazz) throws IOException {
        log.info("Deserializing object from JSON file: {}", jsonFileName);

        // 检查文件是否存在
        File file = new File(jsonFileName);
        if (!file.exists()) {
            log.error("JSON file not found: {}", jsonFileName);
            throw new IOException("JSON file not found: " + jsonFileName);
        }

        // 反序列化
        T obj = OBJECT_MAPPER.readValue(file, clazz);

        log.info("Successfully deserialized object from JSON file: {}", jsonFileName);
        return obj;
    }

    /**
     * 将对象序列化为JSON字符串
     * 
     * @param obj 对象
     * @return JSON字符串
     * @throws IOException 如果序列化失败
     */
    public static String toJsonString(Object obj) throws IOException {
        log.info("Serializing object to JSON string");
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * 从JSON字符串反序列化为对象
     * 
     * @param jsonString JSON字符串
     * @param clazz      目标类
     * @return 对象
     * @throws IOException 如果反序列化失败
     */
    public static <T> T fromJsonString(String jsonString, Class<T> clazz) throws IOException {
        log.info("Deserializing object from JSON string");
        return OBJECT_MAPPER.readValue(jsonString, clazz);
    }

    /**
     * 验证JSON文件格式是否正确
     * 
     * @param jsonFileName JSON文件名
     * @param clazz        目标类
     * @return 如果格式正确返回true，否则返回false
     */
    public static <T> boolean validateJsonFile(String jsonFileName, Class<T> clazz) {
        try {
            fromJsonFile(jsonFileName, clazz);
            return true;
        } catch (IOException e) {
            log.warn("JSON file validation failed: {}", e.getMessage());
            return false;
        }
    }
}

