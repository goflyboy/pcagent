package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品选型项VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSelectionItemVO {
    private Integer rank; // 排序
    private String productCode; // 产品代码
    private String productName; // 产品名称
    private Integer deviationDegree; // 偏离度（百分比，如80表示80%）
    private String description; // 说明（如"内存负偏离"）
}

