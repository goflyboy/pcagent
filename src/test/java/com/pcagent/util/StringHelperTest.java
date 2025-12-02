package com.pcagent.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringHelper 测试类
 */
class StringHelperTest {

    @Test
    void testNormalizeSpecReqItem_NullInput() {
        // 测试 null 输入
        String result = StringHelper.normalizeSpecReqItem(null);
        assertEquals("", result, "null 输入应该返回空字符串");
    }

    @Test
    void testNormalizeSpecReqItem_EmptyString() {
        // 测试空字符串
        String result = StringHelper.normalizeSpecReqItem("");
        assertEquals("", result, "空字符串应该返回空字符串");
    }

    @Test
    void testNormalizeSpecReqItem_WhitespaceOnly() {
        // 测试只有空格的字符串
        String result = StringHelper.normalizeSpecReqItem("   ");
        assertEquals("", result, "只有空格的字符串应该返回空字符串");
    }

    @Test
    void testNormalizeSpecReqItem_ReferenceInput() {
        // 测试参考输入：内存：配置≥256GB DDR4 ECC Registered内存
        String input = "内存：配置≥256GB DDR4 ECC Registered内存";
        String expected = "内存:配置≥256GBDDR4ECCRegistered内存";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "应该去掉空格，中文冒号替换为英文冒号");
    }

    @Test
    void testNormalizeSpecReqItem_RemoveSpaces() {
        // 测试去掉空格
        String input = "CPU: 最新一代 Intel Xeon 处理器";
        String expected = "CPU:最新一代IntelXeon处理器";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "应该去掉所有空格");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseComma() {
        // 测试中文逗号替换
        String input = "内存：256GB，512GB，1024GB";
        String expected = "内存:256GB,512GB,1024GB";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文逗号应该替换为英文逗号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseColon() {
        // 测试中文冒号替换
        String input = "CPU：核心数≥16核";
        String expected = "CPU:核心数≥16核";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文冒号应该替换为英文冒号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseParentheses() {
        // 测试中文括号替换
        String input = "内存（DDR4）配置";
        String expected = "内存(DDR4)配置";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文括号应该替换为英文括号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseBrackets() {
        // 测试中文方括号替换
        String input = "CPU【Intel Xeon】处理器";
        String expected = "CPU[IntelXeon]处理器";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文方括号应该替换为英文方括号，并去掉空格");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseQuotationMarks() {
        // 测试中文引号替换
        String input = "配置" + "\u201C" + "高端" + "\u201D" + "服务器";
        String expected = "配置\"高端\"服务器";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文引号应该替换为英文引号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseSemicolon() {
        // 测试中文分号替换
        String input = "CPU：16核；内存：256GB";
        String expected = "CPU:16核;内存:256GB";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文分号应该替换为英文分号");
    }

    @Test
    void testNormalizeSpecReqItem_ChinesePeriod() {
        // 测试中文句号替换
        String input = "配置完成。";
        String expected = "配置完成.";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文句号应该替换为英文句号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseQuestionMark() {
        // 测试中文问号替换
        String input = "是否需要？";
        String expected = "是否需要?";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文问号应该替换为英文问号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseExclamation() {
        // 测试中文感叹号替换
        String input = "重要！";
        String expected = "重要!";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文感叹号应该替换为英文感叹号");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseDash() {
        // 测试中文破折号替换
        String input = "配置—完成";
        String expected = "配置-完成";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文破折号应该替换为英文短横线");
    }

    @Test
    void testNormalizeSpecReqItem_ChineseEllipsis() {
        // 测试中文省略号替换
        String input = "配置中…";
        String expected = "配置中...";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文省略号应该替换为三个英文句号");
    }

    @Test
    void testNormalizeSpecReqItem_ChinesePauseMark() {
        // 测试中文顿号替换
        String input = "CPU、内存、硬盘";
        String expected = "CPU,内存,硬盘";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中文顿号应该替换为英文逗号");
    }

    @Test
    void testNormalizeSpecReqItem_MultipleSpaces() {
        // 测试多个空格
        String input = "CPU:    16核    内存:    256GB";
        String expected = "CPU:16核内存:256GB";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "多个空格应该全部去掉");
    }

    @Test
    void testNormalizeSpecReqItem_TabAndNewline() {
        // 测试制表符和换行符
        String input = "CPU:\t16核\n内存:\t256GB";
        String expected = "CPU:16核内存:256GB";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "制表符和换行符应该被去掉");
    }

    @Test
    void testNormalizeSpecReqItem_ComplexExample() {
        // 测试复杂示例：包含多种中文标点和空格
        String input = "CPU：最新一代Intel® Xeon® Scalable处理器，核心数≥16核；内存：配置≥256GB DDR4 ECC Registered内存。";
        String expected = "CPU:最新一代Intel®Xeon®Scalable处理器,核心数≥16核;内存:配置≥256GBDDR4ECCRegistered内存.";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "复杂示例应该正确规范化");
    }

    @Test
    void testNormalizeSpecReqItem_NoChangesNeeded() {
        // 测试无需更改的输入（已经是规范化格式）
        String input = "CPU:16核,内存:256GB";
        String expected = "CPU:16核,内存:256GB";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "已经是规范化格式的输入应该保持不变");
    }

    @Test
    void testNormalizeSpecReqItem_SpecialCharacters() {
        // 测试特殊字符（如≥、®等）应该保留
        String input = "CPU：核心数≥16核，Intel®处理器";
        String expected = "CPU:核心数≥16核,Intel®处理器";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "特殊字符（≥、®）应该保留");
    }

    @Test
    void testNormalizeSpecReqItems_NullInput() {
        // 测试 null 列表
        List<String> result = StringHelper.normalizeSpecReqItems(null);
        assertNotNull(result, "null 输入应该返回非 null 列表");
        assertTrue(result.isEmpty(), "null 输入应该返回空列表");
    }

    @Test
    void testNormalizeSpecReqItems_EmptyList() {
        // 测试空列表
        List<String> input = new ArrayList<>();
        List<String> result = StringHelper.normalizeSpecReqItems(input);
        assertNotNull(result, "空列表应该返回非 null 列表");
        assertTrue(result.isEmpty(), "空列表应该返回空列表");
    }

    @Test
    void testNormalizeSpecReqItems_NormalList() {
        // 测试正常列表
        List<String> input = Arrays.asList(
                "CPU：核心数≥16核",
                "内存：配置≥256GB DDR4 ECC Registered内存",
                "硬盘：SSD 1TB"
        );
        List<String> expected = Arrays.asList(
                "CPU:核心数≥16核",
                "内存:配置≥256GBDDR4ECCRegistered内存",
                "硬盘:SSD1TB"
        );
        List<String> result = StringHelper.normalizeSpecReqItems(input);
        assertEquals(expected.size(), result.size(), "列表大小应该相同");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), result.get(i), 
                    "第 " + (i + 1) + " 项应该正确规范化");
        }
    }

    @Test
    void testNormalizeSpecReqItems_WithNullItems() {
        // 测试包含 null 项的列表
        List<String> input = Arrays.asList(
                "CPU：16核",
                null,
                "内存：256GB",
                "",
                "   ",
                "硬盘：1TB"
        );
        List<String> result = StringHelper.normalizeSpecReqItems(input);
        assertEquals(3, result.size(), "应该过滤掉 null、空字符串和只有空格的项");
        assertEquals("CPU:16核", result.get(0));
        assertEquals("内存:256GB", result.get(1));
        assertEquals("硬盘:1TB", result.get(2));
    }

    @Test
    void testNormalize_Method() {
        // 测试 normalize 方法（应该和 normalizeSpecReqItem 行为一致）
        String input = "内存：配置≥256GB DDR4 ECC Registered内存";
        String expected = "内存:配置≥256GBDDR4ECCRegistered内存";
        String result = StringHelper.normalize(input);
        assertEquals(expected, result, "normalize 方法应该和 normalizeSpecReqItem 行为一致");
    }

    @Test
    void testNormalize_NullInput() {
        // 测试 normalize 方法的 null 输入
        String result = StringHelper.normalize(null);
        assertEquals("", result, "null 输入应该返回空字符串");
    }

    @Test
    void testNormalizeSpecReqItem_MixedChineseAndEnglish() {
        // 测试中英文混合
        String input = "CPU：Intel® Xeon® 处理器，核心数≥16核";
        String expected = "CPU:Intel®Xeon®处理器,核心数≥16核";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "中英文混合应该正确处理");
    }

    @Test
    void testNormalizeSpecReqItem_AllChinesePunctuation() {
        // 测试所有中文标点符号
        String input = "测试，。；：（）【】「」" + "\u201C\u201D" + "\u2018\u2019" + "？！、---…";
        String expected = "测试,.;:()[][]\"\"''?!,---...";
        String result = StringHelper.normalizeSpecReqItem(input);
        assertEquals(expected, result, "所有中文标点符号应该正确替换");
    }
}

