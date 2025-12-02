package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数配置项VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfigItemVO {
    private Integer index; // 排序
    private String configReq; // 配置需求（如"CPU:处理器核心数>=16核"）
    private String parameterName; // 参数名称（如"CPU核数"）
    private String parameterCode; // 参数代码（如"CPU_CONFIG"）
    private String value; // 参数值（配置结果，如"16核"）
}

