package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {
    private String code;
    private String type; // 类型：选择性，输入型
    private String defaultValue; // 建议值
    private Integer sortNo; // 排序号
    private List<String> options = new ArrayList<>(); // 可选值列表
    private String refSpecCode; // 引用的规格Code
}

