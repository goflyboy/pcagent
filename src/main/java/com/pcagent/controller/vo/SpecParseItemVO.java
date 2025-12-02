package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格解析项VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecParseItemVO {
    private Integer index; // 序号
    private String originalSpec; // 原始规格需求
    private String stdSpec; // 标准规格需求（格式化后的显示文本）
}

