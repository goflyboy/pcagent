package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数配置意图选项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfigIntentOption {
    private String value; // 意图的值
    private String message; // 意图的信息
    private Boolean isVisited = false;
}

