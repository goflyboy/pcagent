package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数配置结果VO - Step3显示用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfigVO {
    private String productCode; // 产品代码
    private String productName; // 产品名称
    private List<ParameterConfigItemVO> items = new ArrayList<>(); // 参数配置项列表
    private CheckResultVO checkResult; // 检查结果
}

