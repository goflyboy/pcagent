package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数配置结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfig {
    private String code; // 参数code
    private String value; // 配置的结果值
    private String inference; // 推理过程
    private Boolean needCheck = false;
    private String checkPoint; // 确认点
}

