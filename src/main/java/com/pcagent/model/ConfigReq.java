package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置需求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigReq {
    private String productSerial; // 产品系列
    private Integer totalQuantity; // 总套数
    private List<String> specReqItems = new ArrayList<>(); // 规格需求项
    private ConfigStrategy configStrategy = ConfigStrategy.PRICE_MIN_PRIORITY; // 配置策略
    private String totalQuantityMemo; // 总套数说明
    private String country; // 国家/地区
}

