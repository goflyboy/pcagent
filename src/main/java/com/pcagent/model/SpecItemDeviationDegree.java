package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格项偏离度
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecItemDeviationDegree {
    private String originalSpecReq; // 原始的规格需求描述
    private String specName; // 标准的规格名称
    private Boolean satisfy = false; // 是否满足
    private DeviationDegree deviationDegree; // 偏离度
    private Specification stdSpecReq; // 标准规格的信息

    public static SpecItemDeviationDegree buildNotFound(String originalSpec, String specName) {
        SpecItemDeviationDegree result = new SpecItemDeviationDegree();
        result.setOriginalSpecReq(originalSpec);
        result.setSpecName(specName);
        result.setDeviationDegree(DeviationDegree.NOT_FOUND);
        result.setSatisfy(false);
        result.setStdSpecReq(Specification.NOT_FOUND);
        return result;
    }
}

