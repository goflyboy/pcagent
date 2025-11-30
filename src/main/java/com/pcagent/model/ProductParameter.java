package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductParameter {
    private String productCode;
    private List<Parameter> paras = new ArrayList<>();
}

