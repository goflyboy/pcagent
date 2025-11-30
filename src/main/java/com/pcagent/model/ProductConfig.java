package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfig {
    private String productCode;
    private List<ParameterConfig> paras = new ArrayList<>();
    private CheckResult checkResult;
}

