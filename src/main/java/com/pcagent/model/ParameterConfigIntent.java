package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数配置意图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterConfigIntent {
    private String code; // 参数code
    private List<ParameterConfigIntentOption> intentOptions = new ArrayList<>();
    private ParameterConfig result; // 配置结果
    private Parameter base; // 基础数据
}

