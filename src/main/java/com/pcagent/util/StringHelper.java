package com.pcagent.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串处理工具类
 * 用于处理配置需求中的特殊字符和格式问题
 */
public class StringHelper {
    
    /**
     * 规范化规格需求项列表
     * 处理内容：
     * 1. 去掉所有空格
     * 2. 将中文标点符号替换为英文标点符号
     * 3. 统一特殊字符格式
     * 
     * @param specReqItems 原始规格需求项列表
     * @return 规范化后的规格需求项列表
     */
    public static List<String> normalizeSpecReqItems(List<String> specReqItems) {
        if (specReqItems == null || specReqItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> normalized = new ArrayList<>();
        for (String item : specReqItems) {
            if (item != null && !item.trim().isEmpty()) {
                String normalizedItem = normalizeSpecReqItem(item);
                if (!normalizedItem.isEmpty()) {
                    normalized.add(normalizedItem);
                }
            }
        }
        return normalized;
    }
    
    /**
     * 规范化单个规格需求项
     * 
     * @param item 原始规格需求项
     * @return 规范化后的规格需求项
     */
    public static String normalizeSpecReqItem(String item) {
        if (item == null) {
            return "";
        }
        
        String result = item;
        
        // 1. 替换中文标点符号为英文标点符号
        result = replaceChinesePunctuation(result);
        
        // 2. 去掉所有空格
        result = result.replaceAll("\\s+", "");
        
        return result;
    }
    
    /**
     * 替换中文标点符号为英文标点符号
     * 
     * @param text 原始文本
     * @return 替换后的文本
     */
    private static String replaceChinesePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 中文标点符号到英文标点符号的映射
        return text
                // 逗号
                .replace("，", ",")
                .replace("。", ".")
                .replace("；", ";")
                .replace("：", ":")
                // 括号
                .replace("（", "(")
                .replace("）", ")")
                .replace("【", "[")
                .replace("】", "]")
                .replace("「", "[")
                .replace("」", "]")
                // 引号
                .replace("\u201C", "\"")  // 左双引号
                .replace("\u201D", "\"")  // 右双引号
                .replace("\u2018", "'")  // 左单引号
                .replace("\u2019", "'")  // 右单引号
                // 其他符号
                .replace("？", "?")
                .replace("！", "!")
                .replace("、", ",")
                .replace("—", "-")
                .replace("–", "-")
                .replace("…", "...");
    }
    
    /**
     * 规范化单个字符串（去掉空格，替换特殊字符）
     * 
     * @param text 原始文本
     * @return 规范化后的文本
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return normalizeSpecReqItem(text);
    }
}

