package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格偏离度项VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecDeviationItemVO {
    private Integer index; // 序号
    private String originalSpecReq; // 原始规格需求
    private String stdSpecReq; // 标准规格需求
    private String productSpecValue; // 本产品规格值
    private Boolean satisfy; // 是否满足
    private String deviationType; // 偏离情况（正偏离、负偏离、无偏离）
}

