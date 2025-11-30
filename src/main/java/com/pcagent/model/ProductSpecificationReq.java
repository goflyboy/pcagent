package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品级的规格需求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecificationReq {
    private String catalogNode; // 表示本目录下的产品
    private List<SpecificationReq> specReqs = new ArrayList<>();
}

