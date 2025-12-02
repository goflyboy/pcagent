package com.pcagent.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置需求VO - Step1显示用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigReqVO {
    private String productSerial; // 产品系列
    private Integer totalQuantity; // 总套数
    private List<String> specReqItems = new ArrayList<>(); // 规格需求项
    private String configStrategy; // 配置策略（显示名称）
    private String totalQuantityMemo; // 总套数说明
}

