package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品配置意图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfigIntent {
    private String productCode;
    private List<ParameterConfigIntent> paras = new ArrayList<>();
}

