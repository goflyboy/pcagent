package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品选型结果VO - Step2.5显示用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSelectionVO {
    private String selectedProductCode; // 选中的产品代码
    private String selectedProductName; // 选中的产品名称
    private List<ProductSelectionItemVO> candidates = new ArrayList<>(); // 候选产品列表（Top3）
    private List<SpecDeviationItemVO> deviationDetails = new ArrayList<>(); // 选中产品的偏离度详情
}

