package com.pcagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 规格需求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationReq {
    private String originalSpec;
    private List<Specification> stdSpecs = new ArrayList<>(); // 标准规格，满足其中一条就可以
}

